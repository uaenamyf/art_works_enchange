package com.creatorhub.notification.domain.repository;

import com.creatorhub.notification.domain.model.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 通知仓储端口；事件 ID 唯一性保证 Outbox 重投不会产生重复通知。 */
public interface NotificationRepository {

  boolean recordEvent(UUID eventId, String eventType, int eventVersion, Instant consumedAt);

  boolean save(Notification notification);

  List<Notification> findByUser(
      UUID userId, boolean unreadOnly, Instant beforeCreatedAt, UUID beforeId, int limit);

  int countUnread(UUID userId);

  boolean markRead(UUID userId, UUID notificationId, Instant readAt);
}
