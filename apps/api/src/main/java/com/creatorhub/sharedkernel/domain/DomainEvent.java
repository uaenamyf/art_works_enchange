package com.creatorhub.sharedkernel.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 通用领域事件基类。
 *
 * <p>所有领域事件都应使用过去式命名（{@code ArtworkPublished}、{@code OrderCreated}）， 表达已发生的事实，而非命令。
 *
 * <p>实现规范：
 *
 * <ul>
 *   <li>事件字段为不可变值对象，禁止直接持有 PersistenceRow/DO 或框架类型。
 *   <li>{@link #occurredAt()} 必须使用 UTC。
 *   <li>事件版本从 1 开始，消费者至少兼容当前版本与上一版本。
 * </ul>
 */
public abstract class DomainEvent {

  private final UUID eventId;
  private final Instant occurredAt;
  private final int eventVersion;

  protected DomainEvent() {
    this(UUID.randomUUID(), Instant.now(), 1);
  }

  protected DomainEvent(UUID eventId, Instant occurredAt, int eventVersion) {
    this.eventId = Objects.requireNonNull(eventId, "eventId");
    this.occurredAt =
        Objects.requireNonNull(occurredAt, "occurredAt")
            .atZone(java.time.ZoneOffset.UTC)
            .toInstant();
    this.eventVersion = eventVersion;
  }

  public UUID eventId() {
    return eventId;
  }

  public Instant occurredAt() {
    return occurredAt;
  }

  public int eventVersion() {
    return eventVersion;
  }

  /** 事件类型稳定名称，用于序列化与消费者路由。 */
  public abstract String eventType();
}
