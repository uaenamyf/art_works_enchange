package com.creatorhub.identity.interfaces.rest;

import com.creatorhub.identity.application.port.in.AuthRateLimitUseCase;
import com.creatorhub.identity.application.port.in.LoginUseCase;
import com.creatorhub.identity.application.port.in.LoginUseCase.LoginCommand;
import com.creatorhub.identity.application.port.in.LoginUseCase.LoginResult;
import com.creatorhub.identity.application.port.in.RegisterUserUseCase;
import com.creatorhub.identity.application.port.in.RegisterUserUseCase.RegisterCommand;
import com.creatorhub.identity.application.port.in.RegisterUserUseCase.RegistrationResult;
import com.creatorhub.identity.application.port.in.RevokeSessionUseCase;
import com.creatorhub.identity.application.port.in.RotateSessionUseCase;
import com.creatorhub.identity.application.port.in.RotateSessionUseCase.RotateResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
/**
 * Identity 认证 HTTP 适配器，只负责请求校验、协议转换和 Refresh Cookie 写入。
 *
 * <p>Access Token 返回在响应体；Refresh Token 只能进入 HttpOnly Cookie，不能出现在 JSON 中。
 */
public class AuthController {

  private final RegisterUserUseCase registerUser;
  private final AuthRateLimitUseCase rateLimit;
  private final LoginUseCase login;
  private final RotateSessionUseCase rotateSession;
  private final RevokeSessionUseCase revokeSession;
  private final String refreshCookieName;
  private final String refreshCookiePath;
  private final String refreshCookieSameSite;
  private final boolean refreshCookieSecure;
  private final long refreshTtlSeconds;

  public AuthController(
      RegisterUserUseCase registerUser,
      AuthRateLimitUseCase rateLimit,
      LoginUseCase login,
      RotateSessionUseCase rotateSession,
      RevokeSessionUseCase revokeSession,
      @Value("${creatorhub.cookie.refresh.name}") String refreshCookieName,
      @Value("${creatorhub.cookie.refresh.path}") String refreshCookiePath,
      @Value("${creatorhub.cookie.refresh.same-site}") String refreshCookieSameSite,
      @Value("${creatorhub.cookie.refresh.secure}") boolean refreshCookieSecure,
      @Value("${creatorhub.security.jwt.refresh-token-ttl-seconds}") long refreshTtlSeconds) {
    this.registerUser = registerUser;
    this.rateLimit = rateLimit;
    this.login = login;
    this.rotateSession = rotateSession;
    this.revokeSession = revokeSession;
    this.refreshCookieName = refreshCookieName;
    this.refreshCookiePath = refreshCookiePath;
    this.refreshCookieSameSite = refreshCookieSameSite;
    this.refreshCookieSecure = refreshCookieSecure;
    this.refreshTtlSeconds = refreshTtlSeconds;
  }

  @PostMapping("/register")
  /** 注册用户并按 OpenAPI 契约返回 201、Location 与首个认证会话。 */
  ResponseEntity<AuthSessionResponse> register(
      @Valid @RequestBody RegisterRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse response) {
    rateLimit.checkRegistration(servletRequest.getRemoteAddr());
    RegistrationResult result =
        registerUser.register(
            new RegisterCommand(request.email(), request.password(), request.displayName()));
    response.addHeader("Set-Cookie", refreshCookie(result.refreshToken()).toString());
    return ResponseEntity.created(URI.create("/api/v1/users/" + result.userId()))
        .body(AuthSessionResponse.from(result));
  }

  @PostMapping("/login")
  /** 校验邮箱密码并签发新会话；失败信息由应用层统一折叠，防止账户枚举。 */
  ResponseEntity<AuthSessionResponse> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse response) {
    rateLimit.checkLogin(servletRequest.getRemoteAddr(), request.email());
    LoginResult result = login.login(new LoginCommand(request.email(), request.password()));
    response.addHeader("Set-Cookie", refreshCookie(result.refreshToken()).toString());
    return ResponseEntity.ok(AuthSessionResponse.from(result));
  }

  @PostMapping("/refresh")
  /** 消费旧 Refresh Cookie 并返回轮换后的 Access Token 与新 Cookie。 */
  ResponseEntity<AuthSessionResponse> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    rateLimit.checkRefresh(request.getRemoteAddr());
    String refreshToken = readRefreshToken(request);
    RotateResult result = rotateSession.rotate(refreshToken);
    response.addHeader("Set-Cookie", refreshCookie(result.refreshToken()).toString());
    return ResponseEntity.ok(AuthSessionResponse.from(result));
  }

  @PostMapping("/logout")
  /** 幂等撤销当前 Refresh 会话，并用相同属性清除 Cookie。 */
  ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = readRefreshToken(request);
    if (refreshToken != null && !refreshToken.isBlank()) {
      revokeSession.revoke(refreshToken);
    }
    response.addHeader("Set-Cookie", clearRefreshCookie().toString());
    return ResponseEntity.noContent().build();
  }

  private ResponseCookie refreshCookie(String refreshToken) {
    // Cookie 属性统一来自受控配置；生产环境通过 profile 启用 Secure。
    return ResponseCookie.from(refreshCookieName, refreshToken)
        .httpOnly(true)
        .secure(refreshCookieSecure)
        .sameSite(refreshCookieSameSite)
        .path(refreshCookiePath)
        .maxAge(Duration.ofSeconds(refreshTtlSeconds))
        .build();
  }

  private ResponseCookie clearRefreshCookie() {
    return ResponseCookie.from(refreshCookieName, "")
        .httpOnly(true)
        .secure(refreshCookieSecure)
        .sameSite(refreshCookieSameSite)
        .path(refreshCookiePath)
        .maxAge(Duration.ZERO)
        .build();
  }

  private String readRefreshToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (refreshCookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  /** 注册 HTTP 输入模型，字段约束与 OpenAPI RegisterRequest 保持一致。 */
  record RegisterRequest(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 10, max = 128) String password,
      @NotBlank @Size(min = 2, max = 50) String displayName) {}

  /** 登录 HTTP 输入模型；密码不写入日志或错误响应。 */
  record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  /** 认证响应模型，刻意不包含 Refresh Token。 */
  record AuthSessionResponse(String accessToken, long expiresIn, UserSummaryResponse user) {
    static AuthSessionResponse from(RegistrationResult result) {
      return new AuthSessionResponse(
          result.accessToken(),
          result.expiresIn(),
          new UserSummaryResponse(
              result.userId(), result.displayName(), result.avatarUrl(), result.role()));
    }

    static AuthSessionResponse from(LoginResult result) {
      return new AuthSessionResponse(
          result.accessToken(),
          result.expiresIn(),
          new UserSummaryResponse(
              result.userId(), result.displayName(), result.avatarUrl(), result.role()));
    }

    static AuthSessionResponse from(RotateResult result) {
      return new AuthSessionResponse(
          result.accessToken(),
          result.expiresIn(),
          new UserSummaryResponse(
              result.userId(), result.displayName(), result.avatarUrl(), result.role()));
    }
  }

  /** 认证成功后的最小公开用户快照。 */
  record UserSummaryResponse(UUID id, String displayName, String avatarUrl, String role) {}
}
