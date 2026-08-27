package com.creatorhub.payment.application.port.out;

import com.creatorhub.payment.domain.model.PaymentNotification;
import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import com.creatorhub.sharedkernel.domain.Money;
import java.util.UUID;

/** 多渠道支付防腐层；Payment application 只依赖该端口，不接触 Stripe/微信/支付宝 SDK 类型。 */
public interface PaymentProviderPort {

  PaymentProvider provider();

  PaymentSession createPayment(
      UUID paymentId, UUID orderId, String orderNumber, Money amount, String idempotencyKey);

  PaymentSession queryPayment(UUID paymentId, UUID orderId);

  void closePayment(UUID paymentId, UUID orderId);

  void refundPayment(UUID paymentId, UUID orderId, Money amount, String refundIdempotencyKey);

  PaymentNotification verifyNotification(byte[] rawPayload, String signature);
}
