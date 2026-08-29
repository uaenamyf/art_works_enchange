package com.creatorhub.studio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderId;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 Studio 订单应用服务的 owner 隔离和合法履约状态迁移。 */
class StudioOrderServiceTest {

  @Test
  void creatorCanStartProcessingOwnedOrder() {
    OrderRepository orders = mock(OrderRepository.class);
    UUID sellerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    Order order = order(orderId, UUID.randomUUID(), sellerId);
    when(orders.findById(orderId)).thenReturn(java.util.Optional.of(order));
    when(orders.findByIdAndSeller(orderId, sellerId)).thenReturn(java.util.Optional.of(order));
    when(orders.save(order)).thenReturn(order);
    order.markPaid(Instant.parse("2026-08-29T00:01:00Z"));

    var result =
        new StudioOrderService(orders)
            .updateStatus(sellerId, orderId, OrderStatus.PROCESSING, null, null);

    assertThat(result.status()).isEqualTo(OrderStatus.PROCESSING);
    verify(orders).save(order);
  }

  @Test
  void listUsesSellerCursorAndStatus() {
    OrderRepository orders = mock(OrderRepository.class);
    UUID sellerId = UUID.randomUUID();
    when(orders.findBySeller(sellerId, OrderStatus.PAID, null, null, 21)).thenReturn(List.of());

    var result = new StudioOrderService(orders).list(sellerId, OrderStatus.PAID, null, 20);

    assertThat(result.items()).isEmpty();
    verify(orders).findBySeller(sellerId, OrderStatus.PAID, null, null, 21);
  }

  private static Order order(UUID orderId, UUID buyerId, UUID sellerId) {
    return Order.createWithSeller(
        OrderId.of(orderId),
        UserId.of(buyerId),
        UserId.of(sellerId),
        "CH" + orderId.toString().replace("-", "").substring(0, 18),
        new OrderLine(ArtworkId.random(), "Artwork", Money.of("10", "CNY"), 1),
        new ShippingAddress(
            "Buyer", "Line 1", null, "Shanghai", null, "200000", "CN", "13800138000"),
        Instant.parse("2026-08-29T00:00:00Z"),
        Instant.parse("2026-08-29T00:30:00Z"),
        com.creatorhub.ordering.domain.model.FulfillmentType.PHYSICAL);
  }
}
