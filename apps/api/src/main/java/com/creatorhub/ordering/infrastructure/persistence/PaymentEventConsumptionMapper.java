package com.creatorhub.ordering.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 支付事件消费记录 SQL；唯一约束将重复投递转化为无副作用的 false。 */
@Mapper
interface PaymentEventConsumptionMapper {

  @Insert(
      "INSERT INTO ordering_payment_event_consumptions (event_id, order_id, consumed_at) "
          + "VALUES (#{eventId}, #{orderId}, #{consumedAt}) "
          + "ON CONFLICT DO NOTHING")
  int recordPaymentSucceeded(
      @Param("eventId") UUID eventId,
      @Param("orderId") UUID orderId,
      @Param("consumedAt") Instant consumedAt);

  @Insert(
      "INSERT INTO ordering_payment_event_consumptions (event_id, order_id, consumed_at) "
          + "VALUES (#{eventId}, #{orderId}, #{consumedAt}) "
          + "ON CONFLICT DO NOTHING")
  int recordPaymentFailed(
      @Param("eventId") UUID eventId,
      @Param("orderId") UUID orderId,
      @Param("consumedAt") Instant consumedAt);
}
