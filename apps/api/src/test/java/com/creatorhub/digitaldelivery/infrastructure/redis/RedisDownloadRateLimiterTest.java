package com.creatorhub.digitaldelivery.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** 验证数字下载限流器的 Redis Lua 结果和请求标识保护。 */
@Testcontainers
class RedisDownloadRateLimiterTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:8-alpine")).withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redis;

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    redis = new StringRedisTemplate(connectionFactory);
    redis.afterPropertiesSet();
  }

  @AfterEach
  void tearDown() {
    redis.getConnectionFactory().getConnection().serverCommands().flushDb();
    connectionFactory.destroy();
  }

  @Test
  void rejectsWhenSlidingWindowIsExceeded() {
    RedisDownloadRateLimiter limiter =
        new RedisDownloadRateLimiter(redis, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), 10, 60);
    UUID buyerId = UUID.randomUUID();
    String key = "digital-download:grant:" + digest(buyerId + ":203.0.113.8");
    for (int index = 0; index < 10; index++) {
      redis.opsForZSet().add(key, "member-" + index, 0);
    }

    assertThatThrownBy(() -> limiter.check(buyerId, "203.0.113.8"))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("DOWNLOAD_RATE_LIMITED");
  }

  @Test
  void acceptsWithinWindow() {
    RedisDownloadRateLimiter limiter =
        new RedisDownloadRateLimiter(redis, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), 10, 60);

    assertThatCode(() -> limiter.check(UUID.randomUUID(), "203.0.113.8"))
        .doesNotThrowAnyException();
  }

  private static String digest(String value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
