package com.creatorhub.outbox.infrastructure.scheduler;

import com.creatorhub.outbox.application.port.out.OutboxEventPublisher;
import com.creatorhub.outbox.application.service.OutboxRetryPolicy;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Outbox 定时 Worker；逐条发布并按结果确认、重试或进入死信，不静默吞掉失败事件。 */
@Component
public class OutboxWorker {

  private final OutboxRepository outbox;
  private final OutboxEventPublisher publisher;
  private final OutboxRetryPolicy retryPolicy;
  private final int batchSize;
  private final Duration lockDuration;

  public OutboxWorker(
      OutboxRepository outbox,
      OutboxEventPublisher publisher,
      @Value("${creatorhub.outbox.max-attempts:5}") int maxAttempts,
      @Value("${creatorhub.outbox.base-retry-delay-seconds:5}") long baseRetryDelaySeconds,
      @Value("${creatorhub.outbox.max-retry-delay-seconds:3600}") long maxRetryDelaySeconds,
      @Value("${creatorhub.outbox.batch-size:50}") int batchSize,
      @Value("${creatorhub.outbox.lock-duration-seconds:60}") long lockDurationSeconds) {
    this.outbox = outbox;
    this.publisher = publisher;
    this.retryPolicy =
        new OutboxRetryPolicy(
            maxAttempts,
            Duration.ofSeconds(baseRetryDelaySeconds),
            Duration.ofSeconds(maxRetryDelaySeconds));
    this.batchSize = batchSize;
    this.lockDuration = Duration.ofSeconds(lockDurationSeconds);
  }

  @Scheduled(fixedDelayString = "${creatorhub.outbox.poll-delay-ms:1000}")
  public void publishPendingEvents() {
    Instant now = Instant.now();
    for (OutboxEvent event : outbox.claimBatch(now, batchSize, lockDuration)) {
      publishOne(event, now);
    }
  }

  private void publishOne(OutboxEvent event, Instant now) {
    try {
      publisher.publish(event);
      outbox.markDelivered(event.eventId(), Instant.now());
    } catch (RuntimeException exception) {
      String error = errorMessage(exception);
      if (retryPolicy.shouldDeadLetter(event)) {
        outbox.markDead(event.eventId(), now, error);
      } else {
        outbox.scheduleRetry(event.eventId(), retryPolicy.nextAttempt(event, now).value(), error);
      }
    }
  }

  private static String errorMessage(RuntimeException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
    return message.length() <= 2000 ? message : message.substring(0, 2000);
  }
}
