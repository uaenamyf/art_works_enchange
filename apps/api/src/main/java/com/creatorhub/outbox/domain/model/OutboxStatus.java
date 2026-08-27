package com.creatorhub.outbox.domain.model;

/** Outbox 投递状态；DEAD 事件必须保留并告警，不能静默丢弃。 */
public enum OutboxStatus {
  PENDING,
  PROCESSING,
  DELIVERED,
  DEAD
}
