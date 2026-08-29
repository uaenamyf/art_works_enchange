package com.creatorhub.outbox.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.outbox.application.port.out.OutboxEventHandler;
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

  @Test
  void routesRecalledAssetToEverySupportingHandler() {
    OutboxEventHandler notifications = mock(OutboxEventHandler.class);
    OutboxEventHandler digitalDelivery = mock(OutboxEventHandler.class);
    when(notifications.supports("DigitalAssetRecalled", 1)).thenReturn(true);
    when(digitalDelivery.supports("DigitalAssetRecalled", 1)).thenReturn(true);
    OutboxEvent event =
        OutboxEvent.pending(
            UUID.randomUUID(),
            "DigitalAssetRecalled",
            1,
            "DigitalAsset",
            UUID.randomUUID(),
            1,
            Instant.now(),
            null,
            Map.of("assetVersionId", UUID.randomUUID().toString()));

    new LocalOutboxEventPublisher(java.util.List.of(notifications, digitalDelivery)).publish(event);

    verify(notifications).handle(event);
    verify(digitalDelivery).handle(event);
  }
}
