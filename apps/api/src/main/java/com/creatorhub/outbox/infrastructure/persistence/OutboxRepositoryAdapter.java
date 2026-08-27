package com.creatorhub.outbox.infrastructure.persistence;

import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.model.OutboxStatus;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Outbox PostgreSQL 适配器；事件领取和锁定在同一事务中完成，避免并发 Worker 重复消费。 */
@Repository
public class OutboxRepositoryAdapter implements OutboxRepository {

  private final OutboxMapper mapper;
  private final ObjectMapper objectMapper;

  public OutboxRepositoryAdapter(OutboxMapper mapper, ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.objectMapper = objectMapper;
  }

  @Override
  public void append(OutboxEvent event) {
    OutboxPersistenceRow row = toRow(event);
    if (mapper.insert(row) != 1) throw new IllegalStateException("Outbox event insert failed");
  }

  @Override
  @Transactional
  public List<OutboxEvent> claimBatch(Instant now, int limit, Duration lockDuration) {
    int batchSize = Math.min(Math.max(limit, 1), 100);
    Instant lockedUntil = now.plus(Objects.requireNonNull(lockDuration, "lockDuration"));
    return mapper.findClaimable(now, batchSize).stream()
        .filter(row -> mapper.markProcessing(row.getEventId(), lockedUntil) == 1)
        .map(this::toDomain)
        .toList();
  }

  @Override
  public boolean markDelivered(UUID eventId, Instant deliveredAt) {
    return mapper.markDelivered(eventId, deliveredAt) == 1;
  }

  @Override
  public boolean scheduleRetry(UUID eventId, Instant nextAttemptAt, String error) {
    return mapper.scheduleRetry(eventId, nextAttemptAt, error) == 1;
  }

  @Override
  public boolean markDead(UUID eventId, Instant failedAt, String error) {
    return mapper.markDead(eventId, failedAt, error) == 1;
  }

  private OutboxPersistenceRow toRow(OutboxEvent event) {
    OutboxPersistenceRow row = new OutboxPersistenceRow();
    row.setEventId(event.eventId());
    row.setEventType(event.eventType());
    row.setEventVersion(event.eventVersion());
    row.setAggregateType(event.aggregateType());
    row.setAggregateId(event.aggregateId());
    row.setAggregateVersion(event.aggregateVersion());
    row.setOccurredAt(event.occurredAt());
    row.setTraceId(event.traceId());
    row.setPayload(writePayload(event.payload()));
    row.setStatus(event.status());
    row.setAttempts(event.attempts());
    row.setNextAttemptAt(event.nextAttemptAt());
    row.setCreatedAt(event.createdAt());
    return row;
  }

  private OutboxEvent toDomain(OutboxPersistenceRow row) {
    return new OutboxEvent(
        row.getEventId(),
        row.getEventType(),
        row.getEventVersion(),
        row.getAggregateType(),
        row.getAggregateId(),
        row.getAggregateVersion(),
        row.getOccurredAt(),
        row.getTraceId(),
        readPayload(row.getPayload()),
        OutboxStatus.PROCESSING,
        row.getAttempts(),
        row.getNextAttemptAt(),
        row.getLockedUntil(),
        row.getLastError(),
        row.getCreatedAt(),
        row.getDeliveredAt());
  }

  private String writePayload(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Outbox payload serialization failed", exception);
    }
  }

  private Map<String, Object> readPayload(String payload) {
    try {
      return objectMapper.readValue(payload, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Outbox payload deserialization failed", exception);
    }
  }
}
