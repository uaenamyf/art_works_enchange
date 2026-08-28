package com.creatorhub.config;

import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 安全基线：无状态会话、显式公开端点白名单以及业务端点默认认证。
 *
 * <p>仅对明确不依赖 Cookie 身份的健康检查、文档和注册/登录 JSON 端点忽略 CSRF； 新增认证端点时必须重新评估该边界，不能通过扩大通配符快速放行。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CookieOriginValidationFilter cookieOriginValidationFilter;
  private final String allowedOrigins;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      CookieOriginValidationFilter cookieOriginValidationFilter,
      @Value("${creatorhub.cors.allowed-origins}") String allowedOrigins) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.cookieOriginValidationFilter = cookieOriginValidationFilter;
    this.allowedOrigins = allowedOrigins;
  }

  /** 构建全局过滤链；未知业务端点默认需要认证，避免新增 Controller 意外公开。 */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(
            csrf ->
                csrf.ignoringRequestMatchers(
                    "/actuator/health/**",
                    "/v3/api-docs/**",
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout",
                    "/api/v1/me/creator",
                    "/api/v1/media/upload-requests",
                    "/api/v1/media/*/confirm",
                    "/api/v1/favorites/**",
                    "/api/v1/notifications/*/read",
                    "/api/v1/artworks/**"))
        .cors(Customizer.withDefaults())
        .headers(
            headers ->
                headers
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(frame -> frame.deny()))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(new AccessDeniedHandlerImpl()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/v1/creators/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/artworks/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/media/**")
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health/**",
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(cookieOriginValidationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /**
   * 允许受控 Web Origin 携带 HttpOnly Cookie，并暴露 Location 与 Request ID 响应头。 禁止通配 Origin，因为 {@code
   * allowCredentials=true} 时会破坏浏览器安全边界。
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-Request-Id", "X-CSRF-TOKEN"));
    configuration.setExposedHeaders(List.of("Location", "X-Request-Id"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
