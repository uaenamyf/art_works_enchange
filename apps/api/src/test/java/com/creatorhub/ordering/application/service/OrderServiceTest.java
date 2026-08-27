package com.creatorhub.ordering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.StockAvailabilityPort;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.ordering.application.port.in.OrderUseCases.CreateCommand;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyClaim;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyPort;
import com.creatorhub.ordering.application.port.out.StockReservationGate;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderId;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证订单价格快照、库存门禁和幂等键要求。 */
class OrderServiceTest {

  private static final ShippingAddress ADDRESS =
      new ShippingAddress(
          "Alice", "1 Main Street", null, "Shanghai", null, "200000", "CN", "13800138000");

  @Test
  void createsPendingOrderWithCatalogPriceSnapshot() {
    var artworks = mock(ArtworkUseCases.class);
    var stock = mock(StockAvailabilityPort.class);
    var orders = mock(OrderRepository.class);
    var idempotency = mock(OrderIdempotencyPort.class);
    var reservationGate = mock(StockReservationGate.class);
    var service = new OrderService(artworks, stock, orders, idempotency, reservationGate);
    UUID buyerId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(idempotency.claim(
            org.mockito.ArgumentMatchers.eq(buyerId),
            org.mockito.ArgumentMatchers.eq("createOrder"),
            org.mockito.ArgumentMatchers.eq("key"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(new OrderIdempotencyClaim(true, "hash", null));
    when(artworks.getPublished(artworkId))
        .thenReturn(
            new ArtworkUseCases.ArtworkView(
                artworkId,
                UUID.randomUUID(),
                "Vase",
                "Description",
                "ART",
                BigDecimal.TEN,
                "CNY",
                3,
                java.util.List.of(),
                com.creatorhub.catalog.domain.model.ArtworkStatus.PUBLISHED,
                0,
                0,
                false));
    when(stock.reserve(
            org.mockito.ArgumentMatchers.eq(artworkId),
            org.mockito.ArgumentMatchers.eq(2),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    when(reservationGate.reserve(artworkId, 2))
        .thenReturn(new StockReservationGate.Reservation(artworkId, 2, true, false));
    when(orders.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.create(buyerId, new CreateCommand(artworkId, 2, ADDRESS), "key");

    assertThat(result.status())
        .isEqualTo(com.creatorhub.ordering.domain.model.OrderStatus.PENDING_PAYMENT);
    assertThat(result.subtotal()).isEqualByComparingTo("20.00");
    verify(stock)
        .reserve(
            org.mockito.ArgumentMatchers.eq(artworkId),
            org.mockito.ArgumentMatchers.eq(2),
            org.mockito.ArgumentMatchers.any());
    verify(idempotency)
        .complete(
            org.mockito.ArgumentMatchers.eq(buyerId),
            org.mockito.ArgumentMatchers.eq("createOrder"),
            org.mockito.ArgumentMatchers.eq("key"),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsWhenStockCannotBeReserved() {
    var stock = mock(StockAvailabilityPort.class);
    var artworks = mock(ArtworkUseCases.class);
    var idempotency = mock(OrderIdempotencyPort.class);
    var reservationGate = mock(StockReservationGate.class);
    var service =
        new OrderService(
            artworks, stock, mock(OrderRepository.class), idempotency, reservationGate);
    UUID artworkId = UUID.randomUUID();
    UUID buyerId = UUID.randomUUID();
    when(idempotency.claim(
            org.mockito.ArgumentMatchers.eq(buyerId),
            org.mockito.ArgumentMatchers.eq("createOrder"),
            org.mockito.ArgumentMatchers.eq("key"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(new OrderIdempotencyClaim(true, "hash", null));
    when(artworks.getPublished(artworkId))
        .thenReturn(
            new ArtworkUseCases.ArtworkView(
                artworkId,
                UUID.randomUUID(),
                "Vase",
                "Description",
                "ART",
                BigDecimal.TEN,
                "CNY",
                0,
                java.util.List.of(),
                com.creatorhub.catalog.domain.model.ArtworkStatus.PUBLISHED,
                0,
                0,
                false));
    when(stock.reserve(
            org.mockito.ArgumentMatchers.eq(artworkId),
            org.mockito.ArgumentMatchers.eq(1),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);
    when(reservationGate.reserve(artworkId, 1))
        .thenReturn(new StockReservationGate.Reservation(artworkId, 1, true, false));

    assertThatThrownBy(
            () -> service.create(buyerId, new CreateCommand(artworkId, 1, ADDRESS), "key"))
        .isInstanceOf(com.creatorhub.sharedkernel.error.BusinessError.class)
        .extracting(error -> ((com.creatorhub.sharedkernel.error.BusinessError) error).code())
        .isEqualTo("INSUFFICIENT_STOCK");
  }

  @Test
  void rejectsSameKeyWithDifferentRequestHash() {
    var idempotency = mock(OrderIdempotencyPort.class);
    var service =
        new OrderService(
            mock(ArtworkUseCases.class),
            mock(StockAvailabilityPort.class),
            mock(OrderRepository.class),
            idempotency,
            mock(StockReservationGate.class));
    UUID buyerId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(idempotency.claim(
            org.mockito.ArgumentMatchers.eq(buyerId),
            org.mockito.ArgumentMatchers.eq("createOrder"),
            org.mockito.ArgumentMatchers.eq("key"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(new OrderIdempotencyClaim(false, "stored-hash", null));

    assertThatThrownBy(
            () -> service.create(buyerId, new CreateCommand(artworkId, 1, ADDRESS), "key"))
        .isInstanceOf(com.creatorhub.sharedkernel.error.BusinessError.class)
        .extracting(error -> ((com.creatorhub.sharedkernel.error.BusinessError) error).code())
        .isEqualTo("IDEMPOTENCY_KEY_REUSED");
  }

  @Test
  void cancelsPendingOrderAndReleasesStockOnce() {
    var orders = mock(OrderRepository.class);
    var stock = mock(StockAvailabilityPort.class);
    var service =
        new OrderService(
            mock(ArtworkUseCases.class),
            stock,
            orders,
            mock(OrderIdempotencyPort.class),
            mock(StockReservationGate.class));
    UUID buyerId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    Order order = pendingOrder(buyerId, artworkId);
    when(orders.findById(order.id().value())).thenReturn(Optional.of(order));
    when(orders.cancel(order)).thenReturn(true);

    var result = service.cancel(buyerId, order.id().value());

    assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
    verify(orders).cancel(order);
    verify(stock)
        .release(
            org.mockito.ArgumentMatchers.eq(artworkId),
            org.mockito.ArgumentMatchers.eq(2),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void doesNotReleaseStockWhenConcurrentCancelAlreadyWon() {
    var orders = mock(OrderRepository.class);
    var stock = mock(StockAvailabilityPort.class);
    var service =
        new OrderService(
            mock(ArtworkUseCases.class),
            stock,
            orders,
            mock(OrderIdempotencyPort.class),
            mock(StockReservationGate.class));
    UUID buyerId = UUID.randomUUID();
    Order order = pendingOrder(buyerId, UUID.randomUUID());
    when(orders.findById(order.id().value())).thenReturn(Optional.of(order), Optional.of(order));
    when(orders.cancel(order)).thenReturn(false);

    service.cancel(buyerId, order.id().value());

    verify(stock, never())
        .release(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void expiresPendingOrdersAndReleasesStockOnlyAfterConditionalCancel() {
    var orders = mock(OrderRepository.class);
    var stock = mock(StockAvailabilityPort.class);
    var service =
        new OrderService(
            mock(ArtworkUseCases.class),
            stock,
            orders,
            mock(OrderIdempotencyPort.class),
            mock(StockReservationGate.class));
    UUID buyerId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    Order order = pendingOrder(buyerId, artworkId);
    when(orders.findExpiredPending(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(100)))
        .thenReturn(java.util.List.of(order));
    when(orders.cancel(order)).thenReturn(true);

    int expired = service.expirePendingOrders(Instant.parse("2026-08-27T01:00:00Z"), 100);

    assertThat(expired).isEqualTo(1);
    verify(stock)
        .release(
            org.mockito.ArgumentMatchers.eq(artworkId),
            org.mockito.ArgumentMatchers.eq(2),
            org.mockito.ArgumentMatchers.any());
  }

  private static Order pendingOrder(UUID buyerId, UUID artworkId) {
    Instant createdAt = Instant.parse("2026-08-27T00:00:00Z");
    return Order.create(
        OrderId.random(),
        UserId.of(buyerId),
        "CH202608270001",
        new OrderLine(ArtworkId.of(artworkId), "Vase", Money.of("10.00", "CNY"), 2),
        ADDRESS,
        createdAt,
        createdAt.plusSeconds(1800));
  }
}
