package com.creatorhub.notification.application.port.in;

import com.creatorhub.notification.domain.model.Notification;
import java.util.List;
import java.util.UUID;

/** 站内通知公开用例；所有查询和写入都以认证用户 ID 作为权限边界。 */
public interface NotificationUseCases {

  NotificationPage list(UUID userId, String cursor, int limit, boolean unreadOnly);

  void markRead(UUID userId, UUID notificationId);

  record NotificationPage(
      List<Notification> items, String nextCursor, boolean hasMore, int unreadCount) {}
}
