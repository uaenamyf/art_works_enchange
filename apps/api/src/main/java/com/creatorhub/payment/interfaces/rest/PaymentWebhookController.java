package com.creatorhub.payment.interfaces.rest;

import com.creatorhub.payment.application.port.in.PaymentWebhookUseCase;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PayPal Webhook HTTP 适配器；保留原始请求体并转发官方验签所需传输头。 */
@RestController
@RequestMapping("/api/v1/payments/webhooks/paypal")
public class PaymentWebhookController {

  private final PaymentWebhookUseCase webhooks;

  public PaymentWebhookController(PaymentWebhookUseCase webhooks) {
    this.webhooks = webhooks;
  }

  @PostMapping
  ResponseEntity<Void> handle(
      @RequestBody byte[] rawPayload,
      @RequestHeader("Paypal-Transmission-Id") String transmissionId,
      @RequestHeader("Paypal-Transmission-Time") String transmissionTime,
      @RequestHeader("Paypal-Transmission-Sig") String transmissionSignature,
      @RequestHeader("Paypal-Cert-Url") String certUrl,
      @RequestHeader("Paypal-Auth-Algo") String authAlgo) {
    Map<String, String> headers = new HashMap<>();
    headers.put("paypal-transmission-id", transmissionId);
    headers.put("paypal-transmission-time", transmissionTime);
    headers.put("paypal-transmission-sig", transmissionSignature);
    headers.put("paypal-cert-url", certUrl);
    headers.put("paypal-auth-algo", authAlgo);
    webhooks.handlePayPal(rawPayload, headers);
    return ResponseEntity.ok().build();
  }
}
