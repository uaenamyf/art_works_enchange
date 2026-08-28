package com.creatorhub.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.creatorhub.notification.domain.model.NotificationType;
import java.time.Instant;
import java.util.UUID;

/** 通知持久化行，仅供 Notification infrastructure 层使用。 */
@TableName("notifications")
class NotificationPersistenceRow {

  private UUID id;
  private UUID userId;
  private UUID eventId;
  private NotificationType type;
  private String title;
  private String body;
  private String link;
  private Instant readAt;
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public void setReadAt(Instant readAt) {
    this.readAt = readAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
