package com.creatorhub.notification.infrastructure.persistence;

import com.creatorhub.notification.domain.model.Notification;
import com.creatorhub.notification.domain.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 通知仓储适配器；显式转换数据库行，避免基础设施类型泄漏到领域层。 */
@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

  private final NotificationMapper mapper;

  public NotificationRepositoryAdapter(NotificationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean recordEvent(UUID eventId, String eventType, int eventVersion, Instant consumedAt) {
    return mapper.recordEvent(eventId, eventType, eventVersion, consumedAt) == 1;
  }

  @Override
  public boolean save(Notification notification) {
    return mapper.insert(toRow(notification)) == 1;
  }

  @Override
  public List<Notification> findByUser(
      UUID userId, boolean unreadOnly, Instant beforeCreatedAt, UUID beforeId, int limit) {
    return mapper.findByUser(userId, unreadOnly, beforeCreatedAt, beforeId, limit).stream()
        .map(NotificationRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public int countUnread(UUID userId) {
    return mapper.countUnread(userId);
  }

  @Override
  public boolean markRead(UUID userId, UUID notificationId, Instant readAt) {
    return mapper.markRead(userId, notificationId, readAt) == 1;
  }

  private static Notification toDomain(NotificationPersistenceRow row) {
    return new Notification(
        row.getId(),
        row.getUserId(),
        row.getEventId(),
        row.getType(),
        row.getTitle(),
        row.getBody(),
        row.getLink(),
        row.getReadAt(),
        row.getCreatedAt());
  }

  private static NotificationPersistenceRow toRow(Notification notification) {
    NotificationPersistenceRow row = new NotificationPersistenceRow();
    row.setId(notification.id());
    row.setUserId(notification.userId());
    row.setEventId(notification.eventId());
    row.setType(notification.type());
    row.setTitle(notification.title());
    row.setBody(notification.body());
    row.setLink(notification.link());
    row.setReadAt(notification.readAt());
    row.setCreatedAt(notification.createdAt());
    return row;
  }
}
