package com.creatorhub.identity.infrastructure.security;

import com.creatorhub.identity.application.port.out.SessionIssuer;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.infrastructure.persistence.SessionStore;
import com.creatorhub.sharedkernel.domain.AppClock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 会话签发适配器：签名短期 Access Token，并持久化不透明 Refresh Token 的摘要。
 *
 * <p>Refresh Token 明文只返回给接口层写入 HttpOnly Cookie，数据库仅保存 SHA-256 摘要， 降低数据库泄漏后直接接管会话的风险。
 */
@Component
public class JwtSessionIssuer implements SessionIssuer {

  private final SessionStore sessions;
  private final byte[] secret;
  private final String issuer;
  private final long accessTtlSeconds;
  private final long refreshTtlSeconds;

  public JwtSessionIssuer(
      SessionStore sessions,
      @Value("${creatorhub.security.jwt.secret}") String secret,
      @Value("${creatorhub.security.jwt.issuer}") String issuer,
      @Value("${creatorhub.security.jwt.access-token-ttl-seconds}") long accessTtlSeconds,
      @Value("${creatorhub.security.jwt.refresh-token-ttl-seconds}") long refreshTtlSeconds) {
    this.sessions = sessions;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.issuer = issuer;
    this.accessTtlSeconds = accessTtlSeconds;
    this.refreshTtlSeconds = refreshTtlSeconds;
  }

  @Override
  public IssuedSession issue(User user) {
    Instant now = AppClock.now();
    Instant accessExpiresAt = now.plusSeconds(accessTtlSeconds);
    String accessToken =
        Jwts.builder()
            .issuer(issuer)
            .subject(user.id().value().toString())
            .claim("role", user.role().name())
            .claim("displayName", user.displayName())
            .issuedAt(Date.from(now))
            .expiration(Date.from(accessExpiresAt))
            .signWith(Keys.hmacShaKeyFor(secret))
            .compact();

    // 每次签发都生成 384 bit 密码学随机值，不复用旧 Refresh Token。
    String refreshToken = newRefreshToken();
    sessions.create(
        UUID.randomUUID(),
        user.id().value(),
        RefreshTokenDigest.digest(refreshToken),
        now.plusSeconds(refreshTtlSeconds),
        now);
    return new IssuedSession(accessToken, accessTtlSeconds, refreshToken);
  }

  private static String newRefreshToken() {
    byte[] bytes = new byte[48];
    new java.security.SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
