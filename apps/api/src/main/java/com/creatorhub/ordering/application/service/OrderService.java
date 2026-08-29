package com.creatorhub.ordering.application.service;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.StockAvailabilityPort;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.ordering.application.port.in.OrderUseCases;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyClaim;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyPort;
import com.creatorhub.ordering.application.port.out.StockReservationGate;
import com.creatorhub.ordering.domain.model.FulfillmentType;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderId;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final OutboxRepository outbox;
  private final CreatorUseCases creators;

  public OrderService(
      ArtworkUseCases artworks,
      StockAvailabilityPort stock,
      OrderRepository orders,
      OrderIdempotencyPort idempotency,
      StockReservationGate reservationGate) {
    this(artworks, stock, orders, idempotency, reservationGate, null, null);
  }

  public OrderService(
      ArtworkUseCases artworks,
      StockAvailabilityPort stock,
      OrderRepository orders,
      OrderIdempotencyPort idempotency,
      StockReservationGate reservationGate,
      OutboxRepository outbox) {
    this(artworks, stock, orders, idempotency, reservationGate, outbox, null);
  }

  @Autowired
  public OrderService(
      ArtworkUseCases artworks,
      StockAvailabilityPort stock,
      OrderRepository orders,
      OrderIdempotencyPort idempotency,
      StockReservationGate reservationGate,
      OutboxRepository outbox,
      CreatorUseCases creators) {
    this.artworks = artworks;
    this.stock = stock;
    this.orders = orders;
    this.idempotency = idempotency;
    this.reservationGate = reservationGate;
    this.outbox = outbox;
    this.creators = creators;
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
    FulfillmentType fulfillmentType = fulfillmentType(artwork.productType());
    if (fulfillmentType == FulfillmentType.DIGITAL && command.quantity() != 1) {
      throw new BusinessError(
          "DIGITAL_ORDER_QUANTITY_INVALID", "Digital order quantity must be one");
    }
    StockReservationGate.Reservation reservation = null;
    if (fulfillmentType == FulfillmentType.PHYSICAL) {
      reservation = reservationGate.reserve(command.artworkId(), command.quantity());
      if (!reservation.allowed()) {
        throw new BusinessError("INSUFFICIENT_STOCK", "Artwork stock is insufficient");
      }
    }
    try {
      UUID sellerId = creators == null ? null : creators.get(artwork.creatorId()).userId();
      Order order = buildOrder(buyerId, sellerId, command, artwork, fulfillmentType);
      if (fulfillmentType == FulfillmentType.PHYSICAL
          && !stock.reserve(command.artworkId(), command.quantity(), order.id().value())) {
        throw new BusinessError("INSUFFICIENT_STOCK", "Artwork stock is insufficient");
      }
      Order saved = orders.save(order);
      OrderView response = view(saved);
      appendOrderCreated(saved);
      idempotency.complete(buyerId, OPERATION, idempotencyKey, response);
      return response;
    } catch (RuntimeException exception) {
      if (reservation != null) reservationGate.release(reservation);
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
    appendOrderStatusChanged(order, OrderStatus.PENDING_PAYMENT);
    if (order.fulfillmentType() == FulfillmentType.PHYSICAL) {
      stock.release(
          order.lines().get(0).artworkId().value(),
          order.lines().get(0).quantity(),
          order.id().value());
    }
    return view(order);
  }

  @Override
  public int expirePendingOrders(Instant now, int limit) {
    int batchSize = Math.min(Math.max(limit, 1), 100);
    int expired = 0;
    for (Order order : orders.findExpiredPending(now, batchSize)) {
      order.cancel(now);
      if (orders.cancel(order)) {
        appendOrderStatusChanged(order, OrderStatus.PENDING_PAYMENT);
        if (order.fulfillmentType() == FulfillmentType.PHYSICAL) {
          stock.release(
              order.lines().get(0).artworkId().value(),
              order.lines().get(0).quantity(),
              order.id().value());
        }
        expired++;
      }
    }
    return expired;
  }

  @Override
  @Transactional(readOnly = true)
  public OrderView get(UUID buyerId, UUID orderId) {
    Order order =
        orders
            .findById(orderId)
            .filter(candidate -> candidate.buyerId().value().equals(buyerId))
            .orElseThrow(() -> new BusinessError("ORDER_NOT_FOUND", "Order not found"));
    return view(order);
  }

  @Override
  @Transactional(readOnly = true)
  public OrderPage list(UUID buyerId, OrderStatus status, String cursor, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decode(cursor);
    var results =
        orders.findByBuyer(buyerId, status, decoded.createdAt(), decoded.id(), pageSize + 1);
    boolean hasMore = results.size() > pageSize;
    var page = results.stream().limit(pageSize).toList();
    return new OrderPage(
        page.stream().map(OrderService::view).toList(),
        hasMore ? Cursor.encode(page.get(page.size() - 1)) : null,
        hasMore);
  }

  private static String orderNumber() {
    return "CH" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
  }

  private record Cursor(Instant createdAt, UUID id) {
    static Cursor decode(String value) {
      if (value == null || value.isBlank()) return new Cursor(null, null);
      try {
        String[] parts =
            new String(java.util.Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                .split("\\|", 2);
        return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
      } catch (RuntimeException exception) {
        throw new BusinessError("INVALID_CURSOR", "Cursor is invalid");
      }
    }

    static String encode(Order order) {
      return java.util.Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              (order.createdAt() + "|" + order.id().value()).getBytes(StandardCharsets.UTF_8));
    }
  }

  private static Order buildOrder(
      UUID buyerId,
      UUID sellerId,
      CreateCommand command,
      ArtworkUseCases.ArtworkView artwork,
      FulfillmentType fulfillmentType) {
    Instant now = Instant.now();
    OrderLine line =
        new OrderLine(
            ArtworkId.of(artwork.id()),
            artwork.title(),
            Money.of(artwork.priceAmount().toPlainString(), artwork.priceCurrency()),
            command.quantity());
    return Order.createWithSeller(
        OrderId.random(),
        UserId.of(buyerId),
        sellerId == null ? null : UserId.of(sellerId),
        orderNumber(),
        line,
        command.shippingAddress(),
        now,
        now.plusSeconds(1800),
        fulfillmentType);
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
        order.fulfillmentType(),
        order.trackingNumber(),
        order.carrier(),
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

  private static FulfillmentType fulfillmentType(
      com.creatorhub.catalog.domain.model.ProductType type) {
    return type == com.creatorhub.catalog.domain.model.ProductType.DIGITAL
        ? FulfillmentType.DIGITAL
        : FulfillmentType.PHYSICAL;
  }

  private void appendOrderCreated(Order order) {
    if (outbox == null) return;
    OrderLine line = order.lines().get(0);
    outbox.append(
        OutboxEvent.pending(
            UUID.randomUUID(),
            "OrderCreated",
            1,
            "Order",
            order.id().value(),
            0,
            order.createdAt(),
            UUID.randomUUID(),
            java.util.Map.of(
                "recipientId", order.buyerId().value().toString(),
                "orderId", order.id().value().toString(),
                "total", order.subtotal().amount().toPlainString(),
                "currency", line.unitPrice().currency().getCurrencyCode(),
                "title", "订单已创建",
                "body", "你的订单已创建，等待支付。")));
  }

  private void appendOrderStatusChanged(Order order, OrderStatus previousStatus) {
    if (outbox == null) return;
    outbox.append(
        OutboxEvent.pending(
            UUID.randomUUID(),
            "OrderStatusChanged",
            1,
            "Order",
            order.id().value(),
            order.version(),
            order.updatedAt(),
            UUID.randomUUID(),
            java.util.Map.of(
                "recipientId", order.buyerId().value().toString(),
                "orderId", order.id().value().toString(),
                "fromStatus", previousStatus.name(),
                "toStatus", order.status().name(),
                "title", "订单状态已更新",
                "body", "你的订单已取消。")));
  }
}
