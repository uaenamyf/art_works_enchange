package com.creatorhub.notification.application.service;

import com.creatorhub.notification.application.port.in.NotificationUseCases;
import com.creatorhub.notification.domain.model.Notification;
import com.creatorhub.notification.domain.repository.NotificationRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 通知应用服务；负责游标分页和当前用户边界，通知文案由事件消费者生成。 */
@Service
@Transactional
public class NotificationService implements NotificationUseCases {

  private final NotificationRepository notifications;

  public NotificationService(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @Override
  @Transactional(readOnly = true)
  public NotificationPage list(UUID userId, String cursor, int limit, boolean unreadOnly) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decode(cursor);
    List<Notification> entries =
        notifications.findByUser(
            userId, unreadOnly, decoded.createdAt(), decoded.id(), pageSize + 1);
    boolean hasMore = entries.size() > pageSize;
    List<Notification> items = entries.stream().limit(pageSize).toList();
    String nextCursor =
        hasMore && !items.isEmpty() ? Cursor.encode(items.get(items.size() - 1)) : null;
    return new NotificationPage(items, nextCursor, hasMore, notifications.countUnread(userId));
  }

  @Override
  public void markRead(UUID userId, UUID notificationId) {
    if (!notifications.markRead(userId, notificationId, Instant.now())) {
      throw new BusinessError("NOTIFICATION_NOT_FOUND", "Notification not found");
    }
  }

  private record Cursor(Instant createdAt, UUID id) {
    static Cursor decode(String value) {
      if (value == null || value.isBlank()) return new Cursor(null, null);
      try {
        String[] parts =
            new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                .split("\\|", 2);
        return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
      } catch (RuntimeException ex) {
        throw new BusinessError("INVALID_CURSOR", "Cursor is invalid");
      }
    }

    static String encode(Notification notification) {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              (notification.createdAt() + "|" + notification.id())
                  .getBytes(StandardCharsets.UTF_8));
    }
  }
}
