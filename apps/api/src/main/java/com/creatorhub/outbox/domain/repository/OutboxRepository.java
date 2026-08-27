package com.creatorhub.outbox.domain.repository;

import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Outbox 聚合仓储端口；领取操作必须由 PostgreSQL 在事务内使用 SKIP LOCKED 完成。 */
public interface OutboxRepository {

  void append(OutboxEvent event);

  List<OutboxEvent> claimBatch(Instant now, int limit, Duration lockDuration);

  boolean markDelivered(UUID eventId, Instant deliveredAt);

  boolean scheduleRetry(UUID eventId, Instant nextAttemptAt, String error);

  boolean markDead(UUID eventId, Instant failedAt, String error);
}
