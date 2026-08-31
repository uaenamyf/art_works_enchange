package com.creatorhub.payment.application.port.in;

import java.util.Map;

/** PayPal Webhook 用例；只有 Provider 验签成功的通知才能进入 Ordering。 */
public interface PaymentWebhookUseCase {

  void handlePayPal(byte[] rawPayload, Map<String, String> headers);
}
