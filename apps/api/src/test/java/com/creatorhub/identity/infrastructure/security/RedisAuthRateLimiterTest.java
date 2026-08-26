package com.creatorhub.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** 使用真实 Redis 8 验证 Lua 滑动窗口原子计数和邮箱脱敏键。 */
@Testcontainers
class RedisAuthRateLimiterTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:8-alpine")).withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redis;
  private RedisAuthRateLimiter limiter;

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    redis = new StringRedisTemplate(connectionFactory);
    redis.afterPropertiesSet();
    limiter =
        new RedisAuthRateLimiter(
            redis,
            Clock.fixed(Instant.parse("2026-08-26T00:00:00Z"), ZoneOffset.UTC),
            new RedisAuthRateLimiter.Window(3, 3600),
            new RedisAuthRateLimiter.Window(5, 60),
            new RedisAuthRateLimiter.Window(2, 3600),
            new RedisAuthRateLimiter.Window(30, 60));
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @Test
  void registrationRejectsFourthAttemptAndReturnsRetryAfter() {
    limiter.checkRegistration("127.0.0.1");
    limiter.checkRegistration("127.0.0.1");
    limiter.checkRegistration("127.0.0.1");

    assertThatThrownBy(() -> limiter.checkRegistration("127.0.0.1"))
        .isInstanceOf(BusinessError.class)
        .satisfies(
            error -> {
              BusinessError businessError = (BusinessError) error;
              assertThat(businessError.code()).isEqualTo("RATE_LIMITED");
              assertThat(businessError.details()).containsKey("retryAfterSeconds");
            });
  }

  @Test
  void loginEmailKeyNeverContainsPlainEmail() {
    limiter.checkLogin("127.0.0.2", "Alice@Example.com");

    Set<String> keys = redis.keys("auth:login:email:*");
    assertThat(keys).hasSize(1);
    assertThat(keys.iterator().next()).doesNotContain("alice").doesNotContain("example.com");
  }
}
