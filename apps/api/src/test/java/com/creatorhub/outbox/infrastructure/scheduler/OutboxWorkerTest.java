package com.creatorhub.outbox.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.outbox.application.port.out.OutboxEventPublisher;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.model.OutboxStatus;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 Outbox Worker 的成功确认、失败退避和最大次数死信路径。 */
class OutboxWorkerTest {

  @Test
  void successfulPublishMarksEventDelivered() {
    OutboxRepository outbox = mock(OutboxRepository.class);
    OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);
    OutboxEvent event = event(1);
    when(outbox.claimBatch(any(), eq(50), eq(Duration.ofSeconds(60)))).thenReturn(List.of(event));

    new OutboxWorker(outbox, publisher, 5, 5, 3600, 50, 60).publishPendingEvents();

    verify(publisher).publish(event);
    verify(outbox).markDelivered(eq(event.eventId()), any());
  }

  @Test
  void failedPublishSchedulesRetryBeforeLimit() {
    OutboxRepository outbox = mock(OutboxRepository.class);
    OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);
    OutboxEvent event = event(1);
    when(outbox.claimBatch(any(), eq(50), eq(Duration.ofSeconds(60)))).thenReturn(List.of(event));
    org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
        .when(publisher)
        .publish(event);

    new OutboxWorker(outbox, publisher, 5, 5, 3600, 50, 60).publishPendingEvents();

    verify(outbox).scheduleRetry(eq(event.eventId()), any(), eq("broker unavailable"));
  }

  @Test
  void failedPublishAtLimitMarksDead() {
    OutboxRepository outbox = mock(OutboxRepository.class);
    OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);
    OutboxEvent event = event(5);
    when(outbox.claimBatch(any(), eq(50), eq(Duration.ofSeconds(60)))).thenReturn(List.of(event));
    org.mockito.Mockito.doThrow(new IllegalStateException("permanent failure"))
        .when(publisher)
        .publish(event);

    new OutboxWorker(outbox, publisher, 5, 5, 3600, 50, 60).publishPendingEvents();

    verify(outbox).markDead(eq(event.eventId()), any(), eq("permanent failure"));
  }

  private static OutboxEvent event(int attempts) {
    Instant now = Instant.parse("2026-08-28T00:00:00Z");
    return new OutboxEvent(
        UUID.randomUUID(),
        "OrderCreated",
        1,
        "Order",
        UUID.randomUUID(),
        1,
        now,
        null,
        Map.of(),
        OutboxStatus.PROCESSING,
        attempts,
        now,
        now.plusSeconds(60),
        null,
        now,
        null);
  }
}
