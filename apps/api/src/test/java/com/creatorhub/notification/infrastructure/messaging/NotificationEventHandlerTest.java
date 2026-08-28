package com.creatorhub.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.notification.domain.model.Notification;
import com.creatorhub.notification.domain.repository.NotificationRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 验证 Notification 消费者只处理白名单事件，并对重复事件保持幂等。 */
class NotificationEventHandlerTest {

  private final NotificationRepository notifications =
      org.mockito.Mockito.mock(NotificationRepository.class);
  private final NotificationEventHandler handler = new NotificationEventHandler(notifications);

  @Test
  void consumesOrderStatusChangedOnce() {
    UUID eventId = UUID.randomUUID();
    UUID buyerId = UUID.randomUUID();
    when(notifications.recordEvent(any(), any(), any(Integer.class), any())).thenReturn(true);

    handler.handle(
        event(
            eventId,
            "OrderStatusChanged",
            Map.of(
                "buyerId",
                buyerId.toString(),
                "recipientId",
                buyerId.toString(),
                "orderId",
                UUID.randomUUID().toString(),
                "title",
                "订单状态已更新",
                "body",
                "你的订单状态发生变化。")));

    ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(saved.capture());
    assertThat(saved.getValue().userId()).isEqualTo(buyerId);
  }

  @Test
  void consumesOrderCreatedForBuyer() {
    UUID buyerId = UUID.randomUUID();
    when(notifications.recordEvent(any(), any(), any(Integer.class), any())).thenReturn(true);

    handler.handle(
        event(
            UUID.randomUUID(),
            "OrderCreated",
            Map.of(
                "recipientId",
                buyerId.toString(),
                "orderId",
                UUID.randomUUID().toString(),
                "title",
                "订单已创建",
                "body",
                "你的订单已创建，等待支付。")));

    ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(saved.capture());
    assertThat(saved.getValue().type().name()).isEqualTo("ORDER_CREATED");
    assertThat(saved.getValue().userId()).isEqualTo(buyerId);
  }

  @Test
  void skipsDuplicateEventAfterReceipt() {
    when(notifications.recordEvent(any(), any(), any(Integer.class), any())).thenReturn(false);

    handler.handle(
        event(
            UUID.randomUUID(),
            "PaymentSucceeded",
            Map.of(
                "buyerId",
                UUID.randomUUID().toString(),
                "recipientId",
                UUID.randomUUID().toString(),
                "orderId",
                UUID.randomUUID().toString(),
                "title",
                "支付成功",
                "body",
                "订单已支付。")));

    verify(notifications, never()).save(any());
  }

  private static OutboxEvent event(UUID id, String type, Map<String, Object> payload) {
    return OutboxEvent.pending(
        id,
        type,
        1,
        "Order",
        UUID.randomUUID(),
        1,
        Instant.parse("2026-08-28T00:00:00Z"),
        UUID.randomUUID(),
        payload);
  }
}
