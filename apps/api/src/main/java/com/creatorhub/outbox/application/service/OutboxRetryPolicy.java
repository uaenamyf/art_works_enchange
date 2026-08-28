package com.creatorhub.outbox.application.service;

import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.time.Duration;
import java.util.Objects;

/** Outbox 指数退避策略；达到最大尝试次数后进入死信，避免失败事件无限循环。 */
public final class OutboxRetryPolicy {

  private final int maxAttempts;
  private final Duration baseDelay;
  private final Duration maxDelay;

  public OutboxRetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay) {
    if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
    this.maxAttempts = maxAttempts;
    this.baseDelay = requirePositive(baseDelay, "baseDelay");
    this.maxDelay = requirePositive(maxDelay, "maxDelay");
    if (maxDelay.compareTo(baseDelay) < 0) {
      throw new IllegalArgumentException("maxDelay must not be less than baseDelay");
    }
  }

  public boolean shouldDeadLetter(OutboxEvent event) {
    return event.attempts() >= maxAttempts;
  }

  public InstantWithDelay nextAttempt(OutboxEvent event, java.time.Instant now) {
    long exponent = Math.min(Math.max(event.attempts() - 1L, 0L), 30L);
    long seconds = Math.min(baseDelay.toSeconds() * (1L << exponent), maxDelay.toSeconds());
    return new InstantWithDelay(now.plusSeconds(seconds));
  }

  public record InstantWithDelay(java.time.Instant value) {
    public InstantWithDelay {
      Objects.requireNonNull(value, "value");
    }
  }

  private static Duration requirePositive(Duration value, String name) {
    Duration normalized = Objects.requireNonNull(value, name);
    if (normalized.isZero() || normalized.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return normalized;
  }
}
