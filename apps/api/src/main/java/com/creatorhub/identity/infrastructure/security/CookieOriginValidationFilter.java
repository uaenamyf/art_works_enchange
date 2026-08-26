package com.creatorhub.identity.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cookie 认证端点的 Origin/Referer 校验器，防止第三方站点触发刷新或注销。
 *
 * <p>仅保护 {@code /api/v1/auth/refresh} 与 {@code /api/v1/auth/logout} 的 POST； 允许源来自同一 CORS
 * 配置，缺失或解析失败均拒绝。
 */
@Component
public class CookieOriginValidationFilter extends OncePerRequestFilter {

  private static final Set<String> PROTECTED_PATHS =
      Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout");
  private final Set<String> allowedOrigins;

  public CookieOriginValidationFilter(
      @Value("${creatorhub.cors.allowed-origins}") String allowedOrigins) {
    this.allowedOrigins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"POST".equalsIgnoreCase(request.getMethod())
        || !PROTECTED_PATHS.contains(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestOrigin = request.getHeader("Origin");
    if (requestOrigin == null) {
      requestOrigin = originFromReferer(request.getHeader("Referer"));
    }
    if (requestOrigin == null || !allowedOrigins.contains(requestOrigin)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private static String originFromReferer(String referer) {
    if (referer == null || referer.isBlank()) {
      return null;
    }
    try {
      URI uri = URI.create(referer);
      int port = uri.getPort();
      return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? "" : ":" + port);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
