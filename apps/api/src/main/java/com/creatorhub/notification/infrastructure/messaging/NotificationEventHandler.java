package com.creatorhub.notification.infrastructure.messaging;

import com.creatorhub.notification.domain.model.Notification;
import com.creatorhub.notification.domain.model.NotificationType;
import com.creatorhub.notification.domain.repository.NotificationRepository;
import com.creatorhub.outbox.application.port.out.OutboxEventHandler;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Notification Outbox 消费者；只消费白名单事件，并以事件 ID 保证至少一次投递幂等。 */
@Component
public class NotificationEventHandler implements OutboxEventHandler {

  private final NotificationRepository notifications;

  public NotificationEventHandler(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @Override
  public boolean supports(String eventType, int eventVersion) {
    return eventVersion == 1
        && switch (eventType) {
          case "CreatorFollowed",
                  "ArtworkPublished",
                  "OrderCreated",
                  "OrderStatusChanged",
                  "PaymentSucceeded",
                  "PaymentFailed",
                  "DigitalEntitlementGranted",
                  "DigitalAssetRecalled",
                  "ArtworkReviewApproved",
                  "ArtworkReviewRejected" ->
              true;
          default -> false;
        };
  }

  @Override
  @Transactional
  public void handle(OutboxEvent event) {
    if (!supports(event.eventType(), event.eventVersion())) return;
    if (!notifications.recordEvent(
        event.eventId(), event.eventType(), event.eventVersion(), Instant.now())) return;

    UUID recipientId = recipientId(event);
    NotificationType type = NotificationType.valueOf(toNotificationType(event.eventType()));
    notifications.save(
        new Notification(
            UUID.randomUUID(),
            recipientId,
            event.eventId(),
            type,
            text(event, "title"),
            text(event, "body"),
            relativeLink(event),
            null,
            event.occurredAt()));
  }

  private static UUID recipientId(OutboxEvent event) {
    return UUID.fromString(text(event, "recipientId"));
  }

  private static String toNotificationType(String eventType) {
    return switch (eventType) {
      case "CreatorFollowed" -> "CREATOR_FOLLOWED";
      case "ArtworkPublished" -> "ARTWORK_PUBLISHED";
      case "OrderCreated" -> "ORDER_CREATED";
      case "OrderStatusChanged" -> "ORDER_STATUS_CHANGED";
      case "PaymentSucceeded" -> "PAYMENT_SUCCEEDED";
      case "PaymentFailed" -> "PAYMENT_FAILED";
      case "DigitalEntitlementGranted" -> "DIGITAL_ENTITLEMENT_GRANTED";
      case "DigitalAssetRecalled" -> "DIGITAL_ASSET_RECALLED";
      case "ArtworkReviewApproved" -> "ARTWORK_REVIEW_APPROVED";
      case "ArtworkReviewRejected" -> "ARTWORK_REVIEW_REJECTED";
      default -> throw new IllegalArgumentException("Unsupported notification event");
    };
  }

  private static String text(OutboxEvent event, String key) {
    Object value = event.payload().get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("Notification event payload missing " + key);
    }
    return text;
  }

  private static String relativeLink(OutboxEvent event) {
    return switch (event.eventType()) {
      case "CreatorFollowed" -> "/creators/" + text(event, "creatorId");
      case "OrderCreated",
              "OrderStatusChanged",
              "PaymentSucceeded",
              "PaymentFailed",
              "DigitalEntitlementGranted" ->
          "/orders/" + text(event, "orderId");
      case "ArtworkPublished", "DigitalAssetRecalled" -> "/artworks/" + text(event, "artworkId");
      case "ArtworkReviewApproved", "ArtworkReviewRejected" ->
          "/studio/artworks/" + text(event, "artworkId");
      default -> throw new IllegalArgumentException("Unsupported notification event");
    };
  }
}
