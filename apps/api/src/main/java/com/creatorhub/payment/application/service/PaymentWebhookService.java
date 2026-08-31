package com.creatorhub.payment.application.service;

import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.payment.application.port.in.PaymentWebhookUseCase;
import com.creatorhub.payment.application.port.out.PaymentProviderPort;
import com.creatorhub.payment.domain.model.PaymentNotification;
import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PayPal Webhook 应用服务；校验订单快照后通过 Outbox 发布可信支付事件。 */
@Service
@Transactional
public class PaymentWebhookService implements PaymentWebhookUseCase {

  private final OrderRepository orders;
  private final OutboxRepository outbox;
  private final List<PaymentProviderPort> providers;

  public PaymentWebhookService(
      OrderRepository orders, OutboxRepository outbox, List<PaymentProviderPort> providers) {
    this.orders = orders;
    this.outbox = outbox;
    this.providers = providers;
  }

  @Override
  public void handlePayPal(byte[] rawPayload, Map<String, String> headers) {
    PaymentNotification notification = provider().verifyNotification(rawPayload, headers);
    Order order =
        orders
            .findById(notification.orderId())
            .orElseThrow(() -> new IllegalArgumentException("Payment webhook order not found"));
    if (!order.orderNumber().equals(notification.orderNumber())
        || !order.subtotal().equals(notification.amount())) {
      throw new IllegalArgumentException("Payment webhook order snapshot mismatch");
    }
    String eventType =
        notification.status() == PaymentStatus.SUCCEEDED ? "PaymentSucceeded" : "PaymentFailed";
    String title = notification.status() == PaymentStatus.SUCCEEDED ? "支付成功" : "支付失败";
    String body = notification.status() == PaymentStatus.SUCCEEDED ? "你的订单已完成支付。" : "你的订单支付失败。";
    outbox.append(
        OutboxEvent.pending(
            UUID.randomUUID(),
            eventType,
            1,
            "Payment",
            notification.paymentId(),
            0,
            Instant.now(),
            UUID.randomUUID(),
            Map.of(
                "recipientId", order.buyerId().value().toString(),
                "orderId", notification.orderId().toString(),
                "orderNumber", notification.orderNumber(),
                "amount", notification.amount().amount().toPlainString(),
                "currency", notification.amount().currency().getCurrencyCode(),
                "providerEventId", notification.providerEventId(),
                "title", title,
                "body", body)));
  }

  private PaymentProviderPort provider() {
    return providers.stream()
        .filter(candidate -> candidate.provider() == PaymentProvider.PAYPAL)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("PayPal provider is unavailable"));
  }
}
