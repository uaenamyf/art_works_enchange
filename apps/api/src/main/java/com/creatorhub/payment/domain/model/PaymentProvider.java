package com.creatorhub.payment.domain.model;

/** 支付渠道枚举；渠道实现细节留在 Payment infrastructure 适配器。 */
public enum PaymentProvider {
  STRIPE,
  WECHAT,
  ALIPAY
}
