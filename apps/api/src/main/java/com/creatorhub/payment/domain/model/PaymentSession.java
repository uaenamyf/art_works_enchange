package com.creatorhub.payment.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Provider 无关的支付会话快照；clientPayload 只允许放客户端调起支付所需的非秘密字段。 */
public record PaymentSession(
    UUID paymentId,
    UUID orderId,
    PaymentProvider provider,
    PaymentStatus status,
    Map<String, Object> clientPayload) {

  public PaymentSession {
    Objects.requireNonNull(paymentId, "paymentId");
    Objects.requireNonNull(orderId, "orderId");
    Objects.requireNonNull(provider, "provider");
    Objects.requireNonNull(status, "status");
    clientPayload = Map.copyOf(Objects.requireNonNull(clientPayload, "clientPayload"));
  }
}
