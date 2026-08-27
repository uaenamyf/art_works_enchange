package com.creatorhub.outbox.infrastructure.persistence;

import com.creatorhub.outbox.domain.model.OutboxStatus;
import java.time.Instant;
import java.util.UUID;

/** outbox_events 持久化行，仅在 Outbox infrastructure 层使用。 */
class OutboxPersistenceRow {

  private UUID eventId;
  private String eventType;
  private int eventVersion;
  private String aggregateType;
  private UUID aggregateId;
  private long aggregateVersion;
  private Instant occurredAt;
  private UUID traceId;
  private String payload;
  private OutboxStatus status;
  private int attempts;
  private Instant nextAttemptAt;
  private Instant lockedUntil;
  private String lastError;
  private Instant createdAt;
  private Instant deliveredAt;

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public int getEventVersion() {
    return eventVersion;
  }

  public void setEventVersion(int eventVersion) {
    this.eventVersion = eventVersion;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(UUID aggregateId) {
    this.aggregateId = aggregateId;
  }

  public long getAggregateVersion() {
    return aggregateVersion;
  }

  public void setAggregateVersion(long aggregateVersion) {
    this.aggregateVersion = aggregateVersion;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public UUID getTraceId() {
    return traceId;
  }

  public void setTraceId(UUID traceId) {
    this.traceId = traceId;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxStatus status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void setNextAttemptAt(Instant nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(Instant lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(Instant deliveredAt) {
    this.deliveredAt = deliveredAt;
  }
}
