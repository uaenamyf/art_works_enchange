package com.creatorhub.identity.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.application.port.in.AuthRateLimitUseCase;
import com.creatorhub.identity.application.port.in.LoginUseCase;
import com.creatorhub.identity.application.port.in.LoginUseCase.LoginResult;
import com.creatorhub.identity.application.port.in.RegisterUserUseCase;
import com.creatorhub.identity.application.port.in.RegisterUserUseCase.RegistrationResult;
import com.creatorhub.identity.application.port.in.RevokeSessionUseCase;
import com.creatorhub.identity.application.port.in.RotateSessionUseCase;
import com.creatorhub.identity.application.port.in.RotateSessionUseCase.RotateResult;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import com.creatorhub.sharedkernel.error.BusinessError;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    properties = {
      "creatorhub.cookie.refresh.name=ch_refresh",
      "creatorhub.cookie.refresh.path=/api/v1/auth",
      "creatorhub.cookie.refresh.same-site=Lax",
      "creatorhub.cookie.refresh.secure=false",
      "creatorhub.security.jwt.refresh-token-ttl-seconds=1209600",
      "creatorhub.security.jwt.secret=change-me-please-change-me-please-32bytes",
      "creatorhub.security.jwt.issuer=creatorhub",
      "creatorhub.cors.allowed-origins=http://localhost:3000"
    })
@Import({
  SecurityConfig.class,
  GlobalExceptionHandler.class,
  JwtAuthenticationFilter.class,
  CookieOriginValidationFilter.class
})
/** 验证认证 HTTP 契约：状态码、Location、HttpOnly Cookie 与 Problem Details。 */
class AuthControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean RegisterUserUseCase registerUser;
  @MockitoBean AuthRateLimitUseCase rateLimit;
  @MockitoBean LoginUseCase login;
  @MockitoBean RotateSessionUseCase rotateSession;
  @MockitoBean RevokeSessionUseCase revokeSession;

  @Test
  void registerReturnsCreatedSessionAndHttpOnlyRefreshCookie() throws Exception {
    UUID userId = UUID.randomUUID();
    when(registerUser.register(any()))
        .thenReturn(
            new RegistrationResult(
                "access-token", 900, "refresh-token", userId, "Alice", null, "USER"));

    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"email":"alice@example.com","password":"very-secure-password","displayName":"Alice"}
                                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/users/" + userId))
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("ch_refresh=refresh-token"),
                        org.hamcrest.Matchers.containsString("Path=/api/v1/auth"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"))))
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.expiresIn").value(900))
        .andExpect(jsonPath("$.user.id").value(userId.toString()))
        .andExpect(jsonPath("$.user.role").value("USER"));
  }

  @Test
  void duplicateEmailReturnsProblemDetailsConflict() throws Exception {
    when(registerUser.register(any()))
        .thenThrow(new BusinessError("EMAIL_TAKEN", "Email is already registered"));

    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"email":"alice@example.com","password":"very-secure-password","displayName":"Alice"}
                                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void loginReturnsSessionAndRotatedRefreshCookie() throws Exception {
    UUID userId = UUID.randomUUID();
    when(login.login(any()))
        .thenReturn(
            new LoginResult(
                "access-token", 900, "new-refresh-token", userId, "Alice", null, "USER"));

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"email":"alice@example.com","password":"very-secure-password"}
                                """))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.containsString("ch_refresh=new-refresh-token")))
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.user.id").value(userId.toString()));
  }

  @Test
  void invalidPayloadReturnsFieldErrors() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"email":"not-an-email","password":"short","displayName":"A"}
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.email").exists())
        .andExpect(jsonPath("$.fieldErrors.password").exists())
        .andExpect(jsonPath("$.fieldErrors.displayName").exists());
  }

  @Test
  void refreshRotatesCookieAndReturnsFullAuthSession() throws Exception {
    UUID userId = UUID.randomUUID();
    when(rotateSession.rotate("old-refresh"))
        .thenReturn(
            new RotateResult("new-access", 900, "new-refresh", userId, "Alice", null, "USER"));

    mvc.perform(
            post("/api/v1/auth/refresh")
                .with(csrf())
                .header("Origin", "http://localhost:3000")
                .cookie(new Cookie("ch_refresh", "old-refresh")))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Set-Cookie", org.hamcrest.Matchers.containsString("ch_refresh=new-refresh")))
        .andExpect(jsonPath("$.accessToken").value("new-access"))
        .andExpect(jsonPath("$.user.id").value(userId.toString()));
  }

  @Test
  void logoutRevokesSessionAndClearsCookieWithSamePath() throws Exception {
    mvc.perform(
            post("/api/v1/auth/logout")
                .with(csrf())
                .header("Origin", "http://localhost:3000")
                .cookie(new Cookie("ch_refresh", "refresh-token")))
        .andExpect(status().isNoContent())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("ch_refresh="),
                        org.hamcrest.Matchers.containsString("Path=/api/v1/auth"),
                        org.hamcrest.Matchers.containsString("Max-Age=0"))));

    verify(revokeSession).revoke("refresh-token");
  }

  @Test
  void rateLimitedRegistrationReturnsRetryAfterAndProblemDetails() throws Exception {
    org.mockito.Mockito.doThrow(
            new BusinessError(
                "RATE_LIMITED",
                "Too many authentication attempts",
                java.util.Map.of("retryAfterSeconds", 120L),
                null))
        .when(rateLimit)
        .checkRegistration(any());

    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"email":"alice@example.com","password":"very-secure-password","displayName":"Alice"}
                                        """))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "120"))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.details.retryAfterSeconds").value(120));
  }

  @Test
  void refreshRejectsUntrustedOriginBeforeReadingCookie() throws Exception {
    mvc.perform(
            post("/api/v1/auth/refresh")
                .header("Origin", "https://attacker.example")
                .cookie(new Cookie("ch_refresh", "stolen-refresh")))
        .andExpect(status().isForbidden());
  }
}
