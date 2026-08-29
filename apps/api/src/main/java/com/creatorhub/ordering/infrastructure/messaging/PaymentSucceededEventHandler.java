package com.creatorhub.ordering.infrastructure.messaging;

import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.outbox.application.port.out.OutboxEventHandler;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.sharedkernel.domain.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Ordering 对 PaymentSucceeded v1 的内部消费者；只有已验证的支付事件才能推进订单状态。 */
@Component
public class PaymentSucceededEventHandler implements OutboxEventHandler {

  private final OrderRepository orders;

  public PaymentSucceededEventHandler(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public boolean supports(String eventType, int eventVersion) {
    return "PaymentSucceeded".equals(eventType) && eventVersion == 1;
  }

  @Override
  @Transactional
  public void handle(OutboxEvent event) {
    if (!supports(event.eventType(), event.eventVersion())) return;
    UUID orderId = UUID.fromString(text(event, "orderId"));
    Order order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Payment event order not found"));
    if (!order.orderNumber().equals(text(event, "orderNumber"))) {
      throw new IllegalArgumentException("Payment event order number mismatch");
    }
    Money paidAmount = Money.of(text(event, "amount"), text(event, "currency"));
    if (!order.subtotal().equals(paidAmount)) {
      throw new IllegalArgumentException("Payment event amount mismatch");
    }
    if (order.status() == com.creatorhub.ordering.domain.model.OrderStatus.PENDING_PAYMENT) {
      order.markPaid(event.occurredAt());
      orders.save(order);
    }
  }

  private static String text(OutboxEvent event, String key) {
    Object value = event.payload().get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("Payment event payload missing " + key);
    }
    return text;
  }
}
