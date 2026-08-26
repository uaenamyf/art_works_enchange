package com.creatorhub.config;

import com.creatorhub.sharedkernel.domain.AppClock;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 启动时确保 {@link AppClock} 使用系统 UTC；测试可通过 @BeforeEach 注入固定时钟。
 *
 * <p>同时校验 {@code creatorhub.security.jwt.secret} 不为占位值，避免生产环境 忘设环境变量导致用弱默认密钥签发 Access Token。
 */
@Configuration
public class ClockConfig {

  @Value("${creatorhub.security.jwt.secret:}")
  private String jwtSecret;

  private final Environment env;

  public ClockConfig(Environment env) {
    this.env = env;
  }

  @PostConstruct
  void onStartup() {
    AppClock.reset();
    rejectPlaceholderJwtSecretInProduction();
  }

  private void rejectPlaceholderJwtSecretInProduction() {
    // production 必须拒绝空值、公开占位密钥和不足 32 字节的 HS256 密钥。
    // docker 只描述运行拓扑，不等同生产安全环境；本地容器开发可继续使用占位值。
    boolean isProduction = isProfileActive("production");
    if (!isProduction) {
      return;
    }
    if (jwtSecret == null
        || jwtSecret.isBlank()
        || "change-me-please-change-me-please-32bytes".equals(jwtSecret)) {
      throw new IllegalStateException(
          "creatorhub.security.jwt.secret is the placeholder value. "
              + "Set the JWT_SECRET environment variable before running in production.");
    }
    if (jwtSecret.getBytes().length < 32) {
      throw new IllegalStateException(
          "creatorhub.security.jwt.secret must be at least 32 bytes for HS256.");
    }
  }

  private boolean isProfileActive(String profile) {
    for (String p : env.getActiveProfiles()) {
      if (profile.equalsIgnoreCase(p)) {
        return true;
      }
    }
    return false;
  }
}
