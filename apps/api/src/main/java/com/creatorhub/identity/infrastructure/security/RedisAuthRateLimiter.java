package com.creatorhub.identity.infrastructure.security;

import com.creatorhub.identity.application.port.in.AuthRateLimitUseCase;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis ZSET + Lua 的认证滑动窗口限流器。
 *
 * <p>Lua 在 Redis 单线程中原子完成清理、计数、写入和过期设置，避免并发请求绕过阈值。 邮箱只以 SHA-256 摘要进入 Redis key，禁止在缓存键和日志中保存明文个人信息。
 */
@Component
public class RedisAuthRateLimiter implements AuthRateLimitUseCase {

  private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT =
      new DefaultRedisScript<>(
          """
          local key = KEYS[1]
          local now = tonumber(ARGV[1])
          local window = tonumber(ARGV[2])
          local limit = tonumber(ARGV[3])
          local member = ARGV[4]
          redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
          local count = redis.call('ZCARD', key)
          if count >= limit then
            local earliest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
            if earliest[2] then
              return math.max(1, math.ceil((tonumber(earliest[2]) + window - now) / 1000))
            end
            return math.max(1, math.ceil(window / 1000))
          end
          redis.call('ZADD', key, now, member)
          redis.call('PEXPIRE', key, window)
          return 0
          """,
          Long.class);

  private final StringRedisTemplate redis;
  private final Clock clock;
  private final Window registration;
  private final Window loginIp;
  private final Window loginEmail;
  private final Window refreshIp;

  @Autowired
  public RedisAuthRateLimiter(
      StringRedisTemplate redis,
      @Value("${creatorhub.rate-limit.registration.ip.limit:3}") int registrationLimit,
      @Value("${creatorhub.rate-limit.registration.ip.window-seconds:3600}")
          long registrationWindow,
      @Value("${creatorhub.rate-limit.login.ip.limit:5}") int loginIpLimit,
      @Value("${creatorhub.rate-limit.login.ip.window-seconds:60}") long loginIpWindow,
      @Value("${creatorhub.rate-limit.login.email.limit:10}") int loginEmailLimit,
      @Value("${creatorhub.rate-limit.login.email.window-seconds:3600}") long loginEmailWindow,
      @Value("${creatorhub.rate-limit.refresh.ip.limit:30}") int refreshLimit,
      @Value("${creatorhub.rate-limit.refresh.ip.window-seconds:60}") long refreshWindow) {
    this(
        redis,
        Clock.systemUTC(),
        new Window(registrationLimit, registrationWindow),
        new Window(loginIpLimit, loginIpWindow),
        new Window(loginEmailLimit, loginEmailWindow),
        new Window(refreshLimit, refreshWindow));
  }

  RedisAuthRateLimiter(
      StringRedisTemplate redis,
      Clock clock,
      Window registration,
      Window loginIp,
      Window loginEmail,
      Window refreshIp) {
    this.redis = redis;
    this.clock = clock;
    this.registration = registration;
    this.loginIp = loginIp;
    this.loginEmail = loginEmail;
    this.refreshIp = refreshIp;
  }

  @Override
  public void checkRegistration(String clientIp) {
    consume("auth:register:ip:" + safeIp(clientIp), registration);
  }

  @Override
  public void checkLogin(String clientIp, String email) {
    consume("auth:login:ip:" + safeIp(clientIp), loginIp);
    consume("auth:login:email:" + digestEmail(email), loginEmail);
  }

  @Override
  public void checkRefresh(String clientIp) {
    consume("auth:refresh:ip:" + safeIp(clientIp), refreshIp);
  }

  private void consume(String key, Window window) {
    long now = clock.millis();
    Long retryAfter =
        redis.execute(
            SLIDING_WINDOW_SCRIPT,
            List.of(key),
            Long.toString(now),
            Long.toString(window.windowSeconds() * 1000),
            Integer.toString(window.limit()),
            now + ":" + UUID.randomUUID());
    if (retryAfter != null && retryAfter > 0) {
      throw new BusinessError(
          "RATE_LIMITED",
          "Too many authentication attempts",
          Map.of("retryAfterSeconds", retryAfter),
          null);
    }
  }

  private static String safeIp(String clientIp) {
    if (clientIp == null || clientIp.isBlank()) {
      return "unknown";
    }
    return clientIp.trim().replaceAll("[^0-9a-fA-F:.]", "_");
  }

  private static String digestEmail(String email) {
    String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              MessageDigest.getInstance("SHA-256")
                  .digest(normalized.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  /** 限流窗口配置，构造时拒绝无效阈值，避免错误配置导致所有请求被拒绝。 */
  record Window(int limit, long windowSeconds) {
    Window {
      if (limit < 1 || windowSeconds < 1) {
        throw new IllegalArgumentException("Rate limit window must be positive");
      }
    }
  }
}
