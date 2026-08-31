package com.creatorhub.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderId;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.payment.application.port.out.PaymentProviderPort;
import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import com.creatorhub.payment.domain.model.PaymentStatus;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证支付服务只使用买家自己的订单和 Creator 已绑定的 PayPal merchant_id。 */
class PaymentServiceTest {

  @Test
  void createsPayPalSessionWithCreatorMerchantAccountAndServerOrderAmount() {
    OrderRepository orders = mock(OrderRepository.class);
    CreatorUseCases creators = mock(CreatorUseCases.class);
    PaymentProviderPort paypal = mock(PaymentProviderPort.class);
    UUID buyerId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    Order order = order(orderId, buyerId, sellerId);
    when(orders.findById(orderId)).thenReturn(Optional.of(order));
    when(creators.getPayPalAccount(sellerId))
        .thenReturn(new CreatorUseCases.PayPalAccountView(true, "merchant-123"));
    PaymentSession expected =
        new PaymentSession(
            UUID.randomUUID(),
            orderId,
            PaymentProvider.PAYPAL,
            PaymentStatus.REQUIRES_ACTION,
            java.util.Map.of());
    when(paypal.provider()).thenReturn(PaymentProvider.PAYPAL);
    when(paypal.createPayment(
            any(UUID.class),
            eq(orderId),
            eq(order.orderNumber()),
            eq(order.subtotal()),
            eq("payment-key"),
            eq("merchant-123")))
        .thenReturn(expected);

    PaymentSession result =
        new PaymentService(orders, creators, List.of(paypal))
            .createSession(buyerId, orderId, PaymentProvider.PAYPAL, "payment-key");

    assertThat(result).isEqualTo(expected);
    verify(creators).getPayPalAccount(sellerId);
  }

  @Test
  void rejectsPaymentForMissingCreatorPayPalAccount() {
    OrderRepository orders = mock(OrderRepository.class);
    CreatorUseCases creators = mock(CreatorUseCases.class);
    PaymentProviderPort paypal = mock(PaymentProviderPort.class);
    UUID buyerId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    when(orders.findById(orderId)).thenReturn(Optional.of(order(orderId, buyerId, sellerId)));
    when(creators.getPayPalAccount(sellerId))
        .thenReturn(new CreatorUseCases.PayPalAccountView(false, null));
    when(paypal.provider()).thenReturn(PaymentProvider.PAYPAL);

    assertThatThrownBy(
            () ->
                new PaymentService(orders, creators, List.of(paypal))
                    .createSession(buyerId, orderId, PaymentProvider.PAYPAL, "payment-key"))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("PAYMENT_RECIPIENT_UNAVAILABLE");
  }

  @Test
  void rejectsNonBuyerAsOrderNotFound() {
    OrderRepository orders = mock(OrderRepository.class);
    when(orders.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new PaymentService(orders, mock(CreatorUseCases.class), List.of())
                    .createSession(
                        UUID.randomUUID(), UUID.randomUUID(), PaymentProvider.PAYPAL, "key"))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("ORDER_NOT_FOUND");
  }

  private static Order order(UUID orderId, UUID buyerId, UUID sellerId) {
    return Order.createWithSeller(
        OrderId.of(orderId),
        UserId.of(buyerId),
        UserId.of(sellerId),
        "CH" + orderId.toString().replace("-", "").substring(0, 18),
        new OrderLine(ArtworkId.random(), "Artwork", Money.of("19.00", "USD"), 1),
        new ShippingAddress(
            "Buyer", "Line 1", null, "Shanghai", null, "200000", "CN", "13800138000"),
        Instant.parse("2026-08-31T00:00:00Z"),
        Instant.parse("2026-09-01T00:00:00Z"),
        com.creatorhub.ordering.domain.model.FulfillmentType.PHYSICAL);
  }
}
