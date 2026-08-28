package com.creatorhub.digitaldelivery.infrastructure.redis;

import com.creatorhub.digitaldelivery.application.port.out.DownloadRateLimiter;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** 使用 Redis Lua 滑动窗口限制下载凭证签发，不记录明文买家或 IP。 */
@Component
public class RedisDownloadRateLimiter implements DownloadRateLimiter {

  private static final DefaultRedisScript<Long> SCRIPT =
      new DefaultRedisScript<>(
          """
          local key = KEYS[1]
          local now = tonumber(ARGV[1])
          local window = tonumber(ARGV[2])
          local limit = tonumber(ARGV[3])
          redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
          local count = redis.call('ZCARD', key)
          if count >= limit then
            local earliest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
            return math.max(1, math.ceil((tonumber(earliest[2]) + window - now) / 1000))
          end
          redis.call('ZADD', key, now, ARGV[4])
          redis.call('PEXPIRE', key, window)
          return 0
          """,
          Long.class);

  private final StringRedisTemplate redis;
  private final Clock clock;
  private final int limit;
  private final long windowSeconds;

  @Autowired
  public RedisDownloadRateLimiter(
      StringRedisTemplate redis,
      @Value("${creatorhub.rate-limit.download.limit:10}") int limit,
      @Value("${creatorhub.rate-limit.download.window-seconds:60}") long windowSeconds) {
    this(redis, Clock.systemUTC(), limit, windowSeconds);
  }

  RedisDownloadRateLimiter(StringRedisTemplate redis, Clock clock, int limit, long windowSeconds) {
    if (limit < 1 || windowSeconds < 1) {
      throw new IllegalArgumentException("Download rate limit must be positive");
    }
    this.redis = redis;
    this.clock = clock;
    this.limit = limit;
    this.windowSeconds = windowSeconds;
  }

  @Override
  public void check(UUID buyerId, String requestIp) {
    long now = clock.millis();
    Long retryAfter =
        redis.execute(
            SCRIPT,
            List.of("digital-download:grant:" + digest(buyerId + ":" + requestIp)),
            Long.toString(now),
            Long.toString(windowSeconds * 1000),
            Integer.toString(limit),
            now + ":" + UUID.randomUUID());
    if (retryAfter != null && retryAfter > 0) {
      throw new BusinessError("DOWNLOAD_RATE_LIMITED", "Too many download requests");
    }
  }

  private static String digest(String value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
