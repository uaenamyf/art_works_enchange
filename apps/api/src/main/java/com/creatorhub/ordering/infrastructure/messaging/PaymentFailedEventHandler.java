package com.creatorhub.ordering.infrastructure.messaging;

import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.ordering.domain.repository.PaymentEventConsumptionRepository;
import com.creatorhub.outbox.application.port.out.OutboxEventHandler;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Ordering 对 PaymentFailed v1 的内部消费者；失败支付不回退或修改已存在的订单状态。 */
@Component
public class PaymentFailedEventHandler implements OutboxEventHandler {

  private final OrderRepository orders;
  private final PaymentEventConsumptionRepository consumptions;

  public PaymentFailedEventHandler(
      OrderRepository orders, PaymentEventConsumptionRepository consumptions) {
    this.orders = orders;
    this.consumptions = consumptions;
  }

  @Override
  public boolean supports(String eventType, int eventVersion) {
    return "PaymentFailed".equals(eventType) && eventVersion == 1;
  }

  @Override
  @Transactional
  public void handle(OutboxEvent event) {
    if (!supports(event.eventType(), event.eventVersion())) return;
    UUID orderId = UUID.fromString(text(event, "orderId"));
    orders
        .findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Payment event order not found"));
    consumptions.recordPaymentFailed(event.eventId(), orderId, event.occurredAt());
  }

  private static String text(OutboxEvent event, String key) {
    Object value = event.payload().get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("Payment event payload missing " + key);
    }
    return text;
  }
}
