package com.creatorhub.payment.application.port.in;

import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import java.util.UUID;

/** 支付应用用例；支付金额和收款方均由服务端订单与 Creator 配置决定。 */
public interface PaymentUseCases {

  PaymentSession createSession(
      UUID buyerId, UUID orderId, PaymentProvider provider, String idempotencyKey);

  PaymentSession captureSession(UUID buyerId, UUID orderId, String providerOrderId);
}
