package com.creatorhub.outbox.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证暂停消息总线时本地发布适配器只处理脱敏事件元数据。 */
class LocalOutboxEventPublisherTest {

  @Test
  void publishesEventMetadataWithoutExposingPayload() {
    OutboxEvent event =
        OutboxEvent.pending(
            UUID.randomUUID(),
            "DigitalEntitlementGranted",
            1,
            "Entitlement",
            UUID.randomUUID(),
            1,
            Instant.now(),
            null,
            Map.of("entitlementId", UUID.randomUUID().toString()));

    assertThatCode(() -> new LocalOutboxEventPublisher().publish(event)).doesNotThrowAnyException();
  }
}
