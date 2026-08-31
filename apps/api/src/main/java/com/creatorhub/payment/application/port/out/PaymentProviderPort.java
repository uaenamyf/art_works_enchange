package com.creatorhub.payment.application.port.out;

import com.creatorhub.payment.domain.model.PaymentNotification;
import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import com.creatorhub.sharedkernel.domain.Money;
import java.util.Map;
import java.util.UUID;

/** 多渠道支付防腐层；Payment application 只依赖该端口，不接触 Stripe/微信/支付宝 SDK 类型。 */
public interface PaymentProviderPort {

  PaymentProvider provider();

  PaymentSession createPayment(
      UUID paymentId, UUID orderId, String orderNumber, Money amount, String idempotencyKey);

  /** merchantId 必须来自服务端已验证的 Creator 收款账户，不能由买家请求体提供。 */
  default PaymentSession createPayment(
      UUID paymentId,
      UUID orderId,
      String orderNumber,
      Money amount,
      String idempotencyKey,
      String merchantId) {
    return createPayment(paymentId, orderId, orderNumber, amount, idempotencyKey);
  }

  PaymentSession queryPayment(UUID paymentId, UUID orderId);

  /** 仅在买家完成 PayPal 审批后调用；订单最终状态仍由 PayPal Webhook 驱动。 */
  default PaymentSession capturePayment(UUID paymentId, UUID orderId, String providerOrderId) {
    throw new UnsupportedOperationException("Payment capture is not supported");
  }

  void closePayment(UUID paymentId, UUID orderId);

  void refundPayment(UUID paymentId, UUID orderId, Money amount, String refundIdempotencyKey);

  PaymentNotification verifyNotification(byte[] rawPayload, String signature);

  /** PayPal 官方验签需要完整传输头；默认实现保持其他 Provider 的旧接口兼容。 */
  default PaymentNotification verifyNotification(
      byte[] rawPayload, Map<String, String> transmissionHeaders) {
    return verifyNotification(rawPayload, transmissionHeaders.get("paypal-transmission-sig"));
  }
}
