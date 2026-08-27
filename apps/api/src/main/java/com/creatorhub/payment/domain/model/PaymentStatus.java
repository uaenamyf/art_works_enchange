package com.creatorhub.payment.domain.model;

/** 支付会话状态；订单是否已支付仍由验签后的异步通知驱动。 */
public enum PaymentStatus {
  REQUIRES_ACTION,
  PROCESSING,
  SUCCEEDED,
  FAILED
}
