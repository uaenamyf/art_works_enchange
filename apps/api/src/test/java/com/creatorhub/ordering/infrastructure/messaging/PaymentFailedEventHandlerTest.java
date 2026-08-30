package com.creatorhub.ordering.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.ordering.domain.model.FulfillmentType;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderId;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.ordering.domain.repository.PaymentEventConsumptionRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 PaymentFailed 只记录消费事实，不修改订单状态。 */
class PaymentFailedEventHandlerTest {

  private final OrderRepository orders = org.mockito.Mockito.mock(OrderRepository.class);
  private final PaymentEventConsumptionRepository consumptions =
      org.mockito.Mockito.mock(PaymentEventConsumptionRepository.class);
  private final PaymentFailedEventHandler handler =
      new PaymentFailedEventHandler(orders, consumptions);

  @Test
  void recordsFailureWithoutChangingOrderStatus() {
    Order order = order();
    when(orders.findById(order.id().value())).thenReturn(Optional.of(order));

    handler.handle(event(order));

    assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    verify(consumptions)
        .recordPaymentFailed(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(order.id().value()),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsFailureForUnknownOrder() {
    Order order = order();
    when(orders.findById(order.id().value())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(event(order)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Payment event order not found");
  }

  private static Order order() {
    return Order.createWithSeller(
        OrderId.random(),
        UserId.of(UUID.randomUUID()),
        UserId.of(UUID.randomUUID()),
        "CH202608300001",
        new OrderLine(ArtworkId.of(UUID.randomUUID()), "Vase", Money.of("10.00", "CNY"), 1),
        null,
        Instant.parse("2026-08-30T00:00:00Z"),
        Instant.parse("2026-08-30T00:30:00Z"),
        FulfillmentType.DIGITAL);
  }

  private static OutboxEvent event(Order order) {
    return OutboxEvent.pending(
        UUID.randomUUID(),
        "PaymentFailed",
        1,
        "Payment",
        UUID.randomUUID(),
        1,
        Instant.parse("2026-08-30T00:05:00Z"),
        UUID.randomUUID(),
        Map.of("orderId", order.id().value().toString(), "reasonCode", "DECLINED"));
  }
}
