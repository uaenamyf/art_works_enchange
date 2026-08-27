package com.creatorhub.outbox.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 跨上下文事件投递信封；payload 是已脱敏的稳定事件数据，不持有领域实体或第三方对象。 */
public record OutboxEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    String aggregateType,
    UUID aggregateId,
    long aggregateVersion,
    Instant occurredAt,
    UUID traceId,
    Map<String, Object> payload,
    OutboxStatus status,
    int attempts,
    Instant nextAttemptAt,
    Instant lockedUntil,
    String lastError,
    Instant createdAt,
    Instant deliveredAt) {

  public OutboxEvent {
    Objects.requireNonNull(eventId, "eventId");
    requireText(eventType, "eventType");
    if (eventVersion < 1) throw new IllegalArgumentException("eventVersion must be positive");
    requireText(aggregateType, "aggregateType");
    Objects.requireNonNull(aggregateId, "aggregateId");
    if (aggregateVersion < 0) {
      throw new IllegalArgumentException("aggregateVersion must be non-negative");
    }
    Objects.requireNonNull(occurredAt, "occurredAt");
    payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
    Objects.requireNonNull(status, "status");
    if (attempts < 0) throw new IllegalArgumentException("attempts must be non-negative");
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
    Objects.requireNonNull(createdAt, "createdAt");
  }

  /** 构造尚未领取的事件，首次投递时间默认为发生时间。 */
  public static OutboxEvent pending(
      UUID eventId,
      String eventType,
      int eventVersion,
      String aggregateType,
      UUID aggregateId,
      long aggregateVersion,
      Instant occurredAt,
      UUID traceId,
      Map<String, Object> payload) {
    return new OutboxEvent(
        eventId,
        eventType,
        eventVersion,
        aggregateType,
        aggregateId,
        aggregateVersion,
        occurredAt,
        traceId,
        payload,
        OutboxStatus.PENDING,
        0,
        occurredAt,
        null,
        null,
        occurredAt,
        null);
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
  }
}
