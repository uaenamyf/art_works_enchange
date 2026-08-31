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
  void consumesCreatorFollowedForCreatorOwner() {
    UUID creatorOwnerId = UUID.randomUUID();
    when(notifications.recordEvent(any(), any(), any(Integer.class), any())).thenReturn(true);

    handler.handle(
        event(
            UUID.randomUUID(),
            "CreatorFollowed",
            Map.of(
                "recipientId",
                creatorOwnerId.toString(),
                "creatorId",
                UUID.randomUUID().toString(),
                "title",
                "你有新的关注者",
                "body",
                "有人关注了你的 Creator 主页。")));

    ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(saved.capture());
    assertThat(saved.getValue().userId()).isEqualTo(creatorOwnerId);
    assertThat(saved.getValue().type().name()).isEqualTo("CREATOR_FOLLOWED");
    assertThat(saved.getValue().link()).startsWith("/creators/");
  }

  @Test
  void consumesAssetRecallForOwnerWithoutDownloadSecrets() {
    UUID ownerId = UUID.randomUUID();
    when(notifications.recordEvent(any(), any(), any(Integer.class), any())).thenReturn(true);

    handler.handle(
        event(
            UUID.randomUUID(),
            "DigitalAssetRecalled",
            Map.of(
                "recipientId",
                ownerId.toString(),
                "artworkId",
                UUID.randomUUID().toString(),
                "assetVersionId",
                UUID.randomUUID().toString(),
                "reasonCode",
                "content update",
                "title",
                "数字资产已撤回",
                "body",
                "新的下载授权将被停止。",
                "downloadUrl",
                "https://private.example/secret")));

    ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(saved.capture());
    assertThat(saved.getValue().userId()).isEqualTo(ownerId);
    assertThat(saved.getValue().type().name()).isEqualTo("DIGITAL_ASSET_RECALLED");
    assertThat(saved.getValue().link()).startsWith("/artworks/");
    assertThat(saved.getValue().body()).doesNotContain("private.example", "download");
  }

  @Test
  void consumesArtworkReviewRejectedForCreator() {
    UUID creatorId = UUID.randomUUID();
    when(notifications.recordEvent(any(), any(), any(Integer.class), any())).thenReturn(true);

    handler.handle(
        event(
            UUID.randomUUID(),
            "ArtworkReviewRejected",
            Map.of(
                "recipientId",
                creatorId.toString(),
                "artworkId",
                UUID.randomUUID().toString(),
                "title",
                "作品需要修改",
                "body",
                "请补充作品信息后重新提交。")));

    ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(saved.capture());
    assertThat(saved.getValue().userId()).isEqualTo(creatorId);
    assertThat(saved.getValue().type().name()).isEqualTo("ARTWORK_REVIEW_REJECTED");
    assertThat(saved.getValue().link()).startsWith("/studio/artworks/");
  }

  @Test
  void consumesArtworkReviewApprovedForCreator() {
    UUID creatorId = UUID.randomUUID();
    when(notifications.recordEvent(any(), any(), any(Integer.class), any())).thenReturn(true);

    handler.handle(
        event(
            UUID.randomUUID(),
            "ArtworkReviewApproved",
            Map.of(
                "recipientId",
                creatorId.toString(),
                "artworkId",
                UUID.randomUUID().toString(),
                "title",
                "作品审核通过",
                "body",
                "请前往 Studio 发布作品。")));

    ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(saved.capture());
    assertThat(saved.getValue().userId()).isEqualTo(creatorId);
    assertThat(saved.getValue().type().name()).isEqualTo("ARTWORK_REVIEW_APPROVED");
    assertThat(saved.getValue().link()).startsWith("/studio/artworks/");
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
