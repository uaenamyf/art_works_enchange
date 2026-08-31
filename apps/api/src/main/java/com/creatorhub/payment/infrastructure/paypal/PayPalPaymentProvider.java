package com.creatorhub.payment.infrastructure.paypal;

import com.creatorhub.payment.application.port.out.PaymentProviderPort;
import com.creatorhub.payment.domain.model.PaymentNotification;
import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import com.creatorhub.payment.domain.model.PaymentStatus;
import com.creatorhub.sharedkernel.domain.Money;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** PayPal Orders v2 多方收款适配器；密钥只用于服务端 OAuth，不进入领域对象或接口响应。 */
@Component
public class PayPalPaymentProvider implements PaymentProviderPort {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String clientId;
  private final String clientSecret;
  private final String partnerAttributionId;
  private final String webhookId;

  public PayPalPaymentProvider(
      ObjectMapper objectMapper,
      String baseUrl,
      String clientId,
      String clientSecret,
      String partnerAttributionId) {
    this(objectMapper, baseUrl, clientId, clientSecret, partnerAttributionId, "");
  }

  public PayPalPaymentProvider(
      ObjectMapper objectMapper,
      @Value("${creatorhub.payment.paypal.base-url:https://api-m.sandbox.paypal.com}")
          String baseUrl,
      @Value("${creatorhub.payment.paypal.client-id:}") String clientId,
      @Value("${creatorhub.payment.paypal.client-secret:}") String clientSecret,
      @Value("${creatorhub.payment.paypal.partner-attribution-id:}") String partnerAttributionId,
      @Value("${creatorhub.payment.paypal.webhook-id:}") String webhookId) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.objectMapper = objectMapper;
    this.baseUrl = trimTrailingSlash(baseUrl);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.partnerAttributionId = partnerAttributionId;
    this.webhookId = webhookId;
  }

  @Override
  public PaymentProvider provider() {
    return PaymentProvider.PAYPAL;
  }

  @Override
  public PaymentSession createPayment(
      UUID paymentId, UUID orderId, String orderNumber, Money amount, String idempotencyKey) {
    throw new IllegalArgumentException("PayPal merchant_id is required for marketplace payments");
  }

  @Override
  public PaymentSession createPayment(
      UUID paymentId,
      UUID orderId,
      String orderNumber,
      Money amount,
      String idempotencyKey,
      String merchantId) {
    requireCredentials();
    if (merchantId == null || merchantId.isBlank()) {
      throw new IllegalArgumentException("PayPal merchant_id is required");
    }
    Map<String, Object> purchaseUnit = new LinkedHashMap<>();
    purchaseUnit.put("reference_id", orderNumber);
    purchaseUnit.put("invoice_id", orderNumber);
    purchaseUnit.put("custom_id", paymentId + ":" + orderId);
    purchaseUnit.put(
        "amount",
        Map.of(
            "currency_code", amount.currency().getCurrencyCode(),
            "value", amount.amount().toPlainString()));
    purchaseUnit.put("payee", Map.of("merchant_id", merchantId));
    Map<String, Object> payload =
        Map.of("intent", "CAPTURE", "purchase_units", java.util.List.of(purchaseUnit));

    JsonNode response =
        sendJson("POST", "/v2/checkout/orders", payload, idempotencyKey, merchantId);
    String providerOrderId = requiredText(response, "id");
    String status = response.path("status").asText("CREATED");
    String approvalUrl = findLink(response, "approve");
    Map<String, Object> clientPayload = new LinkedHashMap<>();
    clientPayload.put("providerOrderId", providerOrderId);
    clientPayload.put("status", status);
    if (approvalUrl != null) clientPayload.put("approvalUrl", approvalUrl);
    return new PaymentSession(
        paymentId, orderId, PaymentProvider.PAYPAL, PaymentStatus.REQUIRES_ACTION, clientPayload);
  }

  @Override
  public PaymentSession capturePayment(UUID paymentId, UUID orderId, String providerOrderId) {
    requireCredentials();
    JsonNode response =
        sendJson(
            "POST",
            "/v2/checkout/orders/" + encodePath(providerOrderId) + "/capture",
            Map.of(),
            paymentId.toString(),
            null);
    String status = response.path("status").asText();
    PaymentStatus paymentStatus =
        "COMPLETED".equals(status) ? PaymentStatus.SUCCEEDED : PaymentStatus.PROCESSING;
    return new PaymentSession(
        paymentId,
        orderId,
        PaymentProvider.PAYPAL,
        paymentStatus,
        Map.of("providerOrderId", providerOrderId, "status", status));
  }

  @Override
  public PaymentSession queryPayment(UUID paymentId, UUID orderId) {
    requireCredentials();
    throw new UnsupportedOperationException("PayPal query requires the provider order ID");
  }

  @Override
  public void closePayment(UUID paymentId, UUID orderId) {
    throw new UnsupportedOperationException("PayPal order close is not implemented");
  }

  @Override
  public void refundPayment(
      UUID paymentId, UUID orderId, Money amount, String refundIdempotencyKey) {
    throw new UnsupportedOperationException("PayPal refund is not implemented");
  }

  @Override
  public PaymentNotification verifyNotification(byte[] rawPayload, String signature) {
    throw new UnsupportedOperationException(
        "PayPal webhook verification must use PayPal's verify-webhook-signature API");
  }

  @Override
  public PaymentNotification verifyNotification(
      byte[] rawPayload, Map<String, String> transmissionHeaders) {
    requireCredentials();
    if (webhookId.isBlank()) {
      throw new IllegalStateException("PayPal webhook ID is not configured");
    }
    String transmissionId = requiredHeader(transmissionHeaders, "paypal-transmission-id");
    String transmissionTime = requiredHeader(transmissionHeaders, "paypal-transmission-time");
    String transmissionSig = requiredHeader(transmissionHeaders, "paypal-transmission-sig");
    String certUrl = requiredHeader(transmissionHeaders, "paypal-cert-url");
    String authAlgo = requiredHeader(transmissionHeaders, "paypal-auth-algo");
    JsonNode event;
    try {
      event = parse(new String(rawPayload, StandardCharsets.UTF_8));
    } catch (IOException exception) {
      throw new IllegalArgumentException("PayPal webhook payload is invalid", exception);
    }
    Map<String, Object> verificationPayload =
        Map.of(
            "auth_algo", authAlgo,
            "cert_url", certUrl,
            "transmission_id", transmissionId,
            "transmission_sig", transmissionSig,
            "transmission_time", transmissionTime,
            "webhook_id", webhookId,
            "webhook_event", event);
    JsonNode verification =
        sendJson(
            "POST",
            "/v1/notifications/verify-webhook-signature",
            verificationPayload,
            transmissionId,
            null);
    if (!"SUCCESS".equals(verification.path("verification_status").asText())) {
      throw new IllegalArgumentException("PayPal webhook signature verification failed");
    }
    return toNotification(event);
  }

  private JsonNode sendJson(
      String method, String path, Object payload, String requestId, String merchantId) {
    try {
      String accessToken = accessToken();
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(URI.create(baseUrl + path))
              .timeout(Duration.ofSeconds(20))
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .header("PayPal-Request-Id", requestId);
      if (!partnerAttributionId.isBlank()) {
        builder.header("PayPal-Partner-Attribution-Id", partnerAttributionId);
      }
      if (merchantId != null && !merchantId.isBlank()) {
        builder.header("PayPal-Auth-Assertion", authAssertion(merchantId));
      }
      String body = objectMapper.writeValueAsString(payload);
      HttpRequest request =
          builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JsonNode responseBody = parse(response.body());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "PayPal API request failed with status " + response.statusCode());
      }
      return responseBody;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("PayPal API request interrupted", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("PayPal API request failed", exception);
    }
  }

  private String accessToken() throws IOException, InterruptedException {
    String credentials =
        Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/v1/oauth2/token"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Basic " + credentials)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException(
          "PayPal OAuth request failed with status " + response.statusCode());
    }
    return requiredText(parse(response.body()), "access_token");
  }

  private String authAssertion(String merchantId) {
    return base64Url("{\"alg\":\"none\"}")
        + "."
        + base64Url("{\"iss\":\"" + clientId + "\",\"payer_id\":\"" + merchantId + "\"}")
        + ".";
  }

  private PaymentNotification toNotification(JsonNode event) {
    String eventType = event.path("event_type").asText();
    PaymentStatus status =
        switch (eventType) {
          case "PAYMENT.CAPTURE.COMPLETED" -> PaymentStatus.SUCCEEDED;
          case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.REFUNDED" -> PaymentStatus.FAILED;
          default -> throw new IllegalArgumentException("Unsupported PayPal webhook event");
        };
    JsonNode resource = event.path("resource");
    String[] ids = resource.path("custom_id").asText().split(":", 2);
    if (ids.length != 2) throw new IllegalArgumentException("PayPal webhook custom_id is invalid");
    String orderNumber = resource.path("invoice_id").asText();
    if (orderNumber.isBlank()) {
      orderNumber = resource.path("purchase_units").path(0).path("reference_id").asText();
    }
    if (orderNumber.isBlank()) {
      throw new IllegalArgumentException("PayPal webhook order number is missing");
    }
    JsonNode amount = resource.path("amount");
    return new PaymentNotification(
        PaymentProvider.PAYPAL,
        requiredText(event, "id"),
        UUID.fromString(ids[0]),
        UUID.fromString(ids[1]),
        orderNumber,
        Money.of(requiredText(amount, "value"), requiredText(amount, "currency_code")),
        status);
  }

  private void requireCredentials() {
    if (clientId.isBlank() || clientSecret.isBlank()) {
      throw new IllegalStateException("PayPal client credentials are not configured");
    }
  }

  private JsonNode parse(String body) throws IOException {
    return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
  }

  private static String requiredText(JsonNode node, String field) {
    String value = node.path(field).asText();
    if (value == null || value.isBlank())
      throw new IllegalStateException("PayPal response missing " + field);
    return value;
  }

  private static String findLink(JsonNode node, String relation) {
    for (JsonNode link : node.path("links")) {
      if (relation.equals(link.path("rel").asText())) return link.path("href").asText();
    }
    return null;
  }

  private static String base64Url(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String trimTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private static String encodePath(String value) {
    if (value == null || !value.matches("[A-Za-z0-9_-]+")) {
      throw new IllegalArgumentException("PayPal order ID is invalid");
    }
    return value;
  }

  private static String requiredHeader(Map<String, String> headers, String name) {
    String value = headers.get(name);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
    return value;
  }
}
