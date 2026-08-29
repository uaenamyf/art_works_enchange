package com.creatorhub.ordering.domain.repository;

import java.time.Instant;
import java.util.UUID;

/** Ordering 支付事件消费记录端口；记录与订单状态变更必须在同一事务中提交。 */
public interface PaymentEventConsumptionRepository {

  boolean recordPaymentSucceeded(UUID eventId, UUID orderId, Instant consumedAt);
}
