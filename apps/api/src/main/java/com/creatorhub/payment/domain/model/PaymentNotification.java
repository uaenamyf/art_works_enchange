package com.creatorhub.payment.domain.model;

import com.creatorhub.sharedkernel.domain.Money;
import java.util.Objects;
import java.util.UUID;

/** 已完成验签和解密的支付通知；未经校验的原始载荷不得构造此领域对象。 */
public record PaymentNotification(
    PaymentProvider provider,
    String providerEventId,
    UUID paymentId,
    UUID orderId,
    String orderNumber,
    Money amount,
    PaymentStatus status) {

  public PaymentNotification {
    Objects.requireNonNull(provider, "provider");
    requireText(providerEventId, "providerEventId");
    Objects.requireNonNull(paymentId, "paymentId");
    Objects.requireNonNull(orderId, "orderId");
    requireText(orderNumber, "orderNumber");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(status, "status");
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
  }
}
