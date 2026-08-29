package com.creatorhub.ordering.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.domain.model.ArtworkId;
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

/** 验证可信支付成功事件的金额校验、状态推进和重复事件幂等行为。 */
class PaymentSucceededEventHandlerTest {

  private final OrderRepository orders = org.mockito.Mockito.mock(OrderRepository.class);
  private final PaymentEventConsumptionRepository consumptions =
      org.mockito.Mockito.mock(PaymentEventConsumptionRepository.class);
  private final PaymentSucceededEventHandler handler =
      new PaymentSucceededEventHandler(orders, consumptions);

  @Test
  void marksPendingOrderPaidAfterValidPaymentEvent() {
    Order order = pendingOrder();
    when(orders.findById(order.id().value())).thenReturn(Optional.of(order));
    when(consumptions.recordPaymentSucceeded(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(order.id().value()),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    handler.handle(event(order, "20.00", "CNY"));

    assertThat(order.status()).isEqualTo(OrderStatus.PAID);
    verify(orders).save(order);
  }

  @Test
  void rejectsPaymentAmountMismatch() {
    Order order = pendingOrder();
    when(orders.findById(order.id().value())).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> handler.handle(event(order, "19.00", "CNY")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Payment event amount mismatch");

    assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    verify(orders, never()).save(order);
  }

  @Test
  void repeatedEventAfterPaymentDoesNotSaveAgain() {
    Order order = pendingOrder();
    order.markPaid(Instant.parse("2026-08-29T00:00:00Z"));
    when(orders.findById(order.id().value())).thenReturn(Optional.of(order));
    when(consumptions.recordPaymentSucceeded(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(order.id().value()),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);

    handler.handle(event(order, "20.00", "CNY"));

    assertThat(order.status()).isEqualTo(OrderStatus.PAID);
    verify(orders, never()).save(order);
  }

  private static Order pendingOrder() {
    UUID orderId = UUID.randomUUID();
    return Order.createWithSeller(
        OrderId.of(orderId),
        UserId.of(UUID.randomUUID()),
        UserId.of(UUID.randomUUID()),
        "CH202608290001",
        new OrderLine(ArtworkId.of(UUID.randomUUID()), "Vase", Money.of("10.00", "CNY"), 2),
        null,
        Instant.parse("2026-08-29T00:00:00Z"),
        Instant.parse("2026-08-29T00:30:00Z"),
        com.creatorhub.ordering.domain.model.FulfillmentType.DIGITAL);
  }

  private static OutboxEvent event(Order order, String amount, String currency) {
    return OutboxEvent.pending(
        UUID.randomUUID(),
        "PaymentSucceeded",
        1,
        "Payment",
        UUID.randomUUID(),
        1,
        Instant.parse("2026-08-29T00:05:00Z"),
        UUID.randomUUID(),
        Map.of(
            "orderId",
            order.id().value().toString(),
            "orderNumber",
            order.orderNumber(),
            "amount",
            amount,
            "currency",
            currency));
  }
}
