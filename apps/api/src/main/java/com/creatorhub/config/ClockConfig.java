package com.creatorhub.config;

import com.creatorhub.sharedkernel.domain.AppClock;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 启动时确保 {@link AppClock} 使用系统 UTC；测试可通过 @BeforeEach 注入固定时钟。
 *
 * <p>同时校验 {@code creatorhub.security.jwt.secret} 不为占位值，避免生产环境
 * 忘设环境变量导致用弱默认密钥签发 Access Token。
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
        // SEC-API-002: the application.yml default secret is intentionally a
        // well-known string. If a production deployment forgets to set
        // JWT_SECRET we want to fail loudly rather than silently accept weak
        // signing material. Dev / test profiles keep the placeholder for
        // local convenience.
        boolean isProduction = isProfileActive("production") || isProfileActive("docker");
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
