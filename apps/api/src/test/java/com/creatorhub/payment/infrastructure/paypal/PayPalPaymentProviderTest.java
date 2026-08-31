package com.creatorhub.payment.infrastructure.paypal;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import com.creatorhub.sharedkernel.domain.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 使用本地 HTTP 服务验证 PayPal OAuth、Orders v2 请求和 Creator 收款方隔离。 */
class PayPalPaymentProviderTest {

  private HttpServer server;
  private AtomicReference<String> orderRequest;
  private PayPalPaymentProvider provider;

  @BeforeEach
  void setUp() throws IOException {
    orderRequest = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/v1/oauth2/token",
        exchange -> respond(exchange, 200, "{\"access_token\":\"access-token\"}"));
    server.createContext(
        "/v2/checkout/orders",
        exchange -> {
          orderRequest.set(new String(exchange.getRequestBody().readAllBytes()));
          respond(
              exchange,
              201,
              "{\"id\":\"paypal-order-1\",\"status\":\"CREATED\",\"links\":[{\"rel\":\"approve\",\"href\":\"https://paypal.test/approve\"}]}");
        });
    server.start();
    provider =
        new PayPalPaymentProvider(
            new ObjectMapper(),
            "http://localhost:" + server.getAddress().getPort(),
            "client-id",
            "client-secret",
            "partner-bn");
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void createsPayPalOrderForCreatorMerchant() throws Exception {
    PaymentSession session =
        provider.createPayment(
            java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID(),
            "CH123",
            Money.of("19.00", "USD"),
            "idempotency-key",
            "merchant-123");

    var json = new ObjectMapper().readTree(orderRequest.get());
    assertThat(provider.provider()).isEqualTo(PaymentProvider.PAYPAL);
    assertThat(json.path("intent").asText()).isEqualTo("CAPTURE");
    assertThat(json.at("/purchase_units/0/reference_id").asText()).isEqualTo("CH123");
    assertThat(json.at("/purchase_units/0/amount/currency_code").asText()).isEqualTo("USD");
    assertThat(json.at("/purchase_units/0/amount/value").asText()).isEqualTo("19.00");
    assertThat(json.at("/purchase_units/0/payee/merchant_id").asText()).isEqualTo("merchant-123");
    assertThat(session.clientPayload()).containsEntry("providerOrderId", "paypal-order-1");
    assertThat(session.clientPayload()).containsEntry("approvalUrl", "https://paypal.test/approve");
  }

  private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
      throws IOException {
    byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
