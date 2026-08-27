package com.creatorhub.outbox.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 Outbox 公共信封的字段不变量和 payload 不可变性。 */
class OutboxEventTest {

  @Test
  void pendingEventCopiesPayloadAndStartsAtPending() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", UUID.randomUUID().toString());
    Instant occurredAt = Instant.parse("2026-08-27T00:00:00Z");

    OutboxEvent event =
        OutboxEvent.pending(
            UUID.randomUUID(),
            "OrderCreated",
            1,
            "Order",
            UUID.randomUUID(),
            1,
            occurredAt,
            UUID.randomUUID(),
            payload);
    payload.put("secret", "must-not-leak");

    assertThat(event.status()).isEqualTo(OutboxStatus.PENDING);
    assertThat(event.attempts()).isZero();
    assertThat(event.nextAttemptAt()).isEqualTo(occurredAt);
    assertThat(event.payload()).doesNotContainKey("secret");
    assertThatThrownBy(() -> event.payload().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsInvalidEventVersion() {
    assertThatThrownBy(
            () ->
                OutboxEvent.pending(
                    UUID.randomUUID(),
                    "OrderCreated",
                    0,
                    "Order",
                    UUID.randomUUID(),
                    0,
                    Instant.now(),
                    null,
                    Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("eventVersion must be positive");
  }
}
