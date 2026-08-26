package com.creatorhub.identity.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.application.port.in.GetCurrentUserUseCase;
import com.creatorhub.identity.application.port.in.GetCurrentUserUseCase.UserSummary;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 Bearer JWT 进入安全上下文并驱动 /me 查询，缺失凭据时返回 401。 */
@WebMvcTest(
    controllers = CurrentUserController.class,
    properties = {
      "creatorhub.security.jwt.secret=change-me-please-change-me-please-32bytes",
      "creatorhub.security.jwt.issuer=creatorhub",
      "creatorhub.cors.allowed-origins=http://localhost:3000"
    })
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CookieOriginValidationFilter.class})
class CurrentUserControllerTest {

  private static final String SECRET = "change-me-please-change-me-please-32bytes";

  @Autowired MockMvc mvc;
  @MockitoBean GetCurrentUserUseCase currentUser;

  @Test
  void returnsCurrentUserForValidBearerToken() throws Exception {
    UUID userId = UUID.randomUUID();
    when(currentUser.get(userId)).thenReturn(new UserSummary(userId, "Alice", null, "USER"));

    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void rejectsMissingBearerToken() throws Exception {
    mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
  }

  private static String token(UUID userId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer("creatorhub")
        .subject(userId.toString())
        .claim("role", "USER")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(900)))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }
}
