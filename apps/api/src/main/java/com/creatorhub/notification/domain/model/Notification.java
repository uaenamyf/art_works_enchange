package com.creatorhub.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 用户站内通知聚合；已读操作只能改变当前用户自己的通知。 */
public record Notification(
    UUID id,
    UUID userId,
    UUID eventId,
    NotificationType type,
    String title,
    String body,
    String link,
    Instant readAt,
    Instant createdAt) {

  public Notification {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(type, "type");
    title = required(title, "title");
    body = required(body, "body");
    if (link != null && (!link.startsWith("/") || link.startsWith("//"))) {
      throw new IllegalArgumentException("link must be a relative path");
    }
    Objects.requireNonNull(createdAt, "createdAt");
  }

  public Notification markRead(Instant at) {
    return new Notification(id, userId, eventId, type, title, body, link, at, createdAt);
  }

  private static String required(String value, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException(name + " is required");
    return normalized;
  }
}
