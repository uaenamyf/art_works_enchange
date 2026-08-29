package com.creatorhub.ordering.domain.model;

import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AggregateRoot;
import com.creatorhub.sharedkernel.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** 订单聚合根，冻结价格与地址快照并维护支付、履约和退款状态机。 */
public final class Order extends AggregateRoot<OrderId> {

  private final UserId buyerId;
  private final UserId sellerId;
  private final String orderNumber;
  private final OrderLine line;
  private final ShippingAddress shippingAddress;
  private final FulfillmentType fulfillmentType;
  private final Instant createdAt;
  private final Instant expiresAt;
  private OrderStatus status;
  private Instant updatedAt;
  private String trackingNumber;
  private String carrier;

  private Order(
      OrderId id,
      UserId buyerId,
      UserId sellerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt,
      FulfillmentType fulfillmentType) {
    super(id);
    this.buyerId = Objects.requireNonNull(buyerId, "buyerId");
    this.sellerId = sellerId;
    this.orderNumber = required(orderNumber, "orderNumber");
    this.line = Objects.requireNonNull(line, "line");
    this.fulfillmentType = Objects.requireNonNull(fulfillmentType, "fulfillmentType");
    if (fulfillmentType == FulfillmentType.PHYSICAL && shippingAddress == null) {
      throw new IllegalArgumentException("Physical order requires shippingAddress");
    }
    if (fulfillmentType == FulfillmentType.DIGITAL && shippingAddress != null) {
      throw new IllegalArgumentException("Digital order cannot have shippingAddress");
    }
    this.shippingAddress = shippingAddress;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    if (!expiresAt.isAfter(createdAt)) {
      throw new IllegalArgumentException("expiresAt must be after createdAt");
    }
    this.updatedAt = createdAt;
    this.status = OrderStatus.PENDING_PAYMENT;
  }

  public static Order create(
      OrderId id,
      UserId buyerId,
      UserId sellerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt) {
    return new Order(
        id,
        buyerId,
        sellerId,
        orderNumber,
        line,
        shippingAddress,
        createdAt,
        expiresAt,
        FulfillmentType.PHYSICAL);
  }

  public static Order create(
      OrderId id,
      UserId buyerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt) {
    return create(id, buyerId, null, orderNumber, line, shippingAddress, createdAt, expiresAt);
  }

  public static Order create(
      OrderId id,
      UserId buyerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt,
      FulfillmentType fulfillmentType) {
    return new Order(
        id,
        buyerId,
        null,
        orderNumber,
        line,
        shippingAddress,
        createdAt,
        expiresAt,
        fulfillmentType);
  }

  public static Order createWithSeller(
      OrderId id,
      UserId buyerId,
      UserId sellerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt,
      FulfillmentType fulfillmentType) {
    return new Order(
        id,
        buyerId,
        sellerId,
        orderNumber,
        line,
        shippingAddress,
        createdAt,
        expiresAt,
        fulfillmentType);
  }

  /** 仅供 Ordering 仓储适配器恢复已持久化订单，仍复用构造器校验不可变字段。 */
  public static Order reconstitute(
      OrderId id,
      UserId buyerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt,
      OrderStatus status,
      Instant updatedAt) {
    return reconstitute(
        id,
        buyerId,
        null,
        orderNumber,
        line,
        shippingAddress,
        createdAt,
        expiresAt,
        status,
        updatedAt,
        FulfillmentType.PHYSICAL);
  }

  public static Order reconstitute(
      OrderId id,
      UserId buyerId,
      UserId sellerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt,
      OrderStatus status,
      Instant updatedAt,
      FulfillmentType fulfillmentType) {
    Order order =
        new Order(
            id,
            buyerId,
            sellerId,
            orderNumber,
            line,
            shippingAddress,
            createdAt,
            expiresAt,
            fulfillmentType);
    order.status = Objects.requireNonNull(status, "status");
    order.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    return order;
  }

  public static Order reconstitute(
      OrderId id,
      UserId buyerId,
      UserId sellerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt,
      OrderStatus status,
      Instant updatedAt,
      FulfillmentType fulfillmentType,
      String trackingNumber,
      String carrier) {
    Order order =
        reconstitute(
            id,
            buyerId,
            sellerId,
            orderNumber,
            line,
            shippingAddress,
            createdAt,
            expiresAt,
            status,
            updatedAt,
            fulfillmentType);
    order.trackingNumber = normalizeOptional(trackingNumber, "trackingNumber", 100);
    order.carrier = normalizeOptional(carrier, "carrier", 80);
    return order;
  }

  public void markPaid(Instant at) {
    transition(OrderStatus.PAID, at, OrderStatus.PENDING_PAYMENT);
  }

  public void startProcessing(Instant at) {
    transition(OrderStatus.PROCESSING, at, OrderStatus.PAID);
  }

  public void markShipped(Instant at) {
    markShipped(at, null, null);
  }

  public void markShipped(Instant at, String trackingNumber, String carrier) {
    transition(OrderStatus.SHIPPED, at, OrderStatus.PROCESSING);
    this.trackingNumber = normalizeOptional(trackingNumber, "trackingNumber", 100);
    this.carrier = normalizeOptional(carrier, "carrier", 80);
  }

  public void complete(Instant at) {
    transition(OrderStatus.COMPLETED, at, OrderStatus.SHIPPED);
  }

  public void cancel(Instant at) {
    transition(OrderStatus.CANCELLED, at, OrderStatus.PENDING_PAYMENT);
  }

  public void startRefund(Instant at) {
    if (status != OrderStatus.PAID && status != OrderStatus.PROCESSING) {
      throw new IllegalStateException("Order cannot be refunded from " + status);
    }
    transitionTo(OrderStatus.REFUNDING, at);
  }

  public void completeRefund(Instant at) {
    transition(OrderStatus.REFUNDED, at, OrderStatus.REFUNDING);
  }

  public UserId buyerId() {
    return buyerId;
  }

  public UserId sellerId() {
    return sellerId;
  }

  public String orderNumber() {
    return orderNumber;
  }

  public List<OrderLine> lines() {
    return List.of(line);
  }

  public ShippingAddress shippingAddress() {
    return shippingAddress;
  }

  public FulfillmentType fulfillmentType() {
    return fulfillmentType;
  }

  public Money subtotal() {
    return line.subtotal();
  }

  public OrderStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public String trackingNumber() {
    return trackingNumber;
  }

  public String carrier() {
    return carrier;
  }

  private void transition(OrderStatus target, Instant at, OrderStatus expected) {
    if (status != expected) {
      throw new IllegalStateException("Order cannot transition from " + status + " to " + target);
    }
    transitionTo(target, at);
  }

  private void transitionTo(OrderStatus target, Instant at) {
    status = target;
    updatedAt = Objects.requireNonNull(at, "at");
    bumpVersion();
  }

  private static String required(String value, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(name + " is invalid");
    }
    return normalized;
  }

  private static String normalizeOptional(String value, String name, int maxLength) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(name + " length is invalid");
    }
    return normalized;
  }
}
