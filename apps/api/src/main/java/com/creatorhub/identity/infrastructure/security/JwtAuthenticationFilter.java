package com.creatorhub.identity.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer JWT 认证过滤器，只从 Authorization 头读取短期 Access Token。
 *
 * <p>签名、issuer 和过期时间全部由 JJWT 校验；Refresh Token 只存在于 HttpOnly Cookie， 不参与该过滤器。无效 Token
 * 保持匿名，由后续授权规则统一返回 401。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final byte[] secret;
  private final String issuer;

  public JwtAuthenticationFilter(
      @Value("${creatorhub.security.jwt.secret}") String secret,
      @Value("${creatorhub.security.jwt.issuer}") String issuer) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.issuer = issuer;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      authenticate(header.substring(7));
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(Keys.hmacShaKeyFor(secret))
              .requireIssuer(issuer)
              .build()
              .parseSignedClaims(token)
              .getPayload();
      UUID userId = UUID.fromString(claims.getSubject());
      String role = claims.get("role", String.class);
      var authentication =
          new UsernamePasswordAuthenticationToken(
              userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (RuntimeException ignored) {
      // 不向客户端暴露签名、issuer、过期或格式的具体失败原因，统一按未认证处理。
      SecurityContextHolder.clearContext();
    }
  }
}
