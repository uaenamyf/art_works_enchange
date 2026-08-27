package com.creatorhub.ordering.domain.model;

/** 订单状态机；支付状态只能由 Payment 事件推进，不能由订单 Controller 直接修改。 */
public enum OrderStatus {
  PENDING_PAYMENT,
  PAID,
  PROCESSING,
  SHIPPED,
  COMPLETED,
  CANCELLED,
  REFUNDING,
  REFUNDED
}
