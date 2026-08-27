package com.creatorhub.ordering.application.service;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.StockAvailabilityPort;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.ordering.application.port.in.OrderUseCases;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyClaim;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyPort;
import com.creatorhub.ordering.application.port.out.StockReservationGate;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderId;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 创建订单应用服务；价格来自 Catalog，库存由数据库条件更新裁决，订单初始为待支付。 */
@Service
@Transactional
public class OrderService implements OrderUseCases {

  private static final String OPERATION = "createOrder";
  private final ArtworkUseCases artworks;
  private final StockAvailabilityPort stock;
  private final OrderRepository orders;
  private final OrderIdempotencyPort idempotency;
  private final StockReservationGate reservationGate;

  public OrderService(
      ArtworkUseCases artworks,
      StockAvailabilityPort stock,
      OrderRepository orders,
      OrderIdempotencyPort idempotency,
      StockReservationGate reservationGate) {
    this.artworks = artworks;
    this.stock = stock;
    this.orders = orders;
    this.idempotency = idempotency;
    this.reservationGate = reservationGate;
  }

  @Override
  public OrderView create(UUID buyerId, CreateCommand command, String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new BusinessError("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key is required");
    }
    String requestHash =
        hash(
            buyerId
                + "|"
                + command.artworkId()
                + "|"
                + command.quantity()
                + "|"
                + command.shippingAddress());
    OrderIdempotencyClaim claim =
        idempotency.claim(buyerId, OPERATION, idempotencyKey, requestHash);
    if (!claim.acquired()) {
      if (!requestHash.equals(claim.requestHash())) {
        throw new BusinessError("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was reused");
      }
      if (claim.response() == null) {
        throw new BusinessError(
            "IDEMPOTENCY_REQUEST_IN_PROGRESS", "Idempotency-Key is in progress");
      }
      return claim.response();
    }
    if (command.quantity() < 1 || command.quantity() > 99) {
      throw new BusinessError("ORDER_QUANTITY_INVALID", "Order quantity is invalid");
    }
    var artwork = artworks.getPublished(command.artworkId());
    StockReservationGate.Reservation reservation =
        reservationGate.reserve(command.artworkId(), command.quantity());
    if (!reservation.allowed()) {
      throw new BusinessError("INSUFFICIENT_STOCK", "Artwork stock is insufficient");
    }
    try {
      Order order = buildOrder(buyerId, command, artwork);
      if (!stock.reserve(command.artworkId(), command.quantity(), order.id().value())) {
        throw new BusinessError("INSUFFICIENT_STOCK", "Artwork stock is insufficient");
      }
      Order saved = orders.save(order);
      OrderView response = view(saved);
      idempotency.complete(buyerId, OPERATION, idempotencyKey, response);
      return response;
    } catch (RuntimeException exception) {
      reservationGate.release(reservation);
      throw exception;
    }
  }

  @Override
  public OrderView cancel(UUID buyerId, UUID orderId) {
    Order order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new BusinessError("ORDER_NOT_FOUND", "Order not found"));
    if (!order.buyerId().value().equals(buyerId)) {
      throw new BusinessError("ORDER_FORBIDDEN", "Order does not belong to buyer");
    }
    if (order.status() == com.creatorhub.ordering.domain.model.OrderStatus.CANCELLED) {
      return view(order);
    }
    order.cancel(Instant.now());
    if (!orders.cancel(order)) {
      return orders
          .findById(orderId)
          .map(OrderService::view)
          .orElseThrow(() -> new BusinessError("ORDER_NOT_FOUND", "Order not found"));
    }
    stock.release(
        order.lines().get(0).artworkId().value(),
        order.lines().get(0).quantity(),
        order.id().value());
    return view(order);
  }

  @Override
  public int expirePendingOrders(Instant now, int limit) {
    int batchSize = Math.min(Math.max(limit, 1), 100);
    int expired = 0;
    for (Order order : orders.findExpiredPending(now, batchSize)) {
      order.cancel(now);
      if (orders.cancel(order)) {
        stock.release(
            order.lines().get(0).artworkId().value(),
            order.lines().get(0).quantity(),
            order.id().value());
        expired++;
      }
    }
    return expired;
  }

  private static String orderNumber() {
    return "CH" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
  }

  private static Order buildOrder(
      UUID buyerId, CreateCommand command, ArtworkUseCases.ArtworkView artwork) {
    Instant now = Instant.now();
    OrderLine line =
        new OrderLine(
            ArtworkId.of(artwork.id()),
            artwork.title(),
            Money.of(artwork.priceAmount().toPlainString(), artwork.priceCurrency()),
            command.quantity());
    return Order.create(
        OrderId.random(),
        UserId.of(buyerId),
        orderNumber(),
        line,
        command.shippingAddress(),
        now,
        now.plusSeconds(1800));
  }

  private static OrderView view(Order order) {
    var line = order.lines().get(0);
    return new OrderView(
        order.id().value(),
        order.orderNumber(),
        order.buyerId().value(),
        line.artworkId().value(),
        line.title(),
        line.unitPrice().amount(),
        line.unitPrice().currency().getCurrencyCode(),
        line.quantity(),
        line.subtotal().amount(),
        order.status(),
        order.shippingAddress(),
        order.createdAt(),
        order.updatedAt(),
        order.expiresAt());
  }

  private static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
