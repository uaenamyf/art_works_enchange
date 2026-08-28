package com.creatorhub.outbox.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** Outbox 显式 SQL Mapper；关键领取查询使用 PostgreSQL 行锁和 SKIP LOCKED。 */
@Mapper
interface OutboxMapper {

  @Insert(
      """
      INSERT INTO outbox_events (event_id, event_type, event_version, aggregate_type, aggregate_id,
          aggregate_version, occurred_at, trace_id, payload, status, attempts, next_attempt_at,
          created_at)
      VALUES (#{eventId}, #{eventType}, #{eventVersion}, #{aggregateType}, #{aggregateId},
          #{aggregateVersion}, #{occurredAt}, #{traceId}, CAST(#{payload} AS jsonb), #{status},
          #{attempts}, #{nextAttemptAt}, #{createdAt})
      ON CONFLICT (event_id) DO NOTHING
      """)
  int insert(OutboxPersistenceRow row);

  @Select(
      """
      SELECT *
      FROM outbox_events
      WHERE (status = 'PENDING' AND next_attempt_at <= #{now})
         OR (status = 'PROCESSING' AND locked_until <= #{now})
      ORDER BY occurred_at ASC, event_id ASC
      LIMIT #{limit}
      FOR UPDATE SKIP LOCKED
      """)
  List<OutboxPersistenceRow> findClaimable(Instant now, int limit);

  @Update(
      "UPDATE outbox_events SET status = 'PROCESSING', attempts = attempts + 1, locked_until = #{lockedUntil} WHERE event_id = #{eventId} AND status IN ('PENDING', 'PROCESSING')")
  int markProcessing(UUID eventId, Instant lockedUntil);

  @Update(
      "UPDATE outbox_events SET status = 'DELIVERED', delivered_at = #{deliveredAt}, locked_until = NULL, last_error = NULL WHERE event_id = #{eventId} AND status = 'PROCESSING'")
  int markDelivered(UUID eventId, Instant deliveredAt);

  @Update(
      "UPDATE outbox_events SET status = 'PENDING', next_attempt_at = #{nextAttemptAt}, locked_until = NULL, last_error = #{error} WHERE event_id = #{eventId} AND status = 'PROCESSING'")
  int scheduleRetry(UUID eventId, Instant nextAttemptAt, String error);

  @Update(
      "UPDATE outbox_events SET status = 'DEAD', locked_until = NULL, last_error = #{error} WHERE event_id = #{eventId} AND status = 'PROCESSING'")
  int markDead(UUID eventId, Instant failedAt, String error);
}
