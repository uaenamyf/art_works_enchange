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
  private final String orderNumber;
  private final OrderLine line;
  private final ShippingAddress shippingAddress;
  private final Instant createdAt;
  private final Instant expiresAt;
  private OrderStatus status;
  private Instant updatedAt;

  private Order(
      OrderId id,
      UserId buyerId,
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt) {
    super(id);
    this.buyerId = Objects.requireNonNull(buyerId, "buyerId");
    this.orderNumber = required(orderNumber, "orderNumber");
    this.line = Objects.requireNonNull(line, "line");
    this.shippingAddress = Objects.requireNonNull(shippingAddress, "shippingAddress");
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
      String orderNumber,
      OrderLine line,
      ShippingAddress shippingAddress,
      Instant createdAt,
      Instant expiresAt) {
    return new Order(id, buyerId, orderNumber, line, shippingAddress, createdAt, expiresAt);
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
    Order order = new Order(id, buyerId, orderNumber, line, shippingAddress, createdAt, expiresAt);
    order.status = Objects.requireNonNull(status, "status");
    order.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    return order;
  }

  public void markPaid(Instant at) {
    transition(OrderStatus.PAID, at, OrderStatus.PENDING_PAYMENT);
  }

  public void startProcessing(Instant at) {
    transition(OrderStatus.PROCESSING, at, OrderStatus.PAID);
  }

  public void markShipped(Instant at) {
    transition(OrderStatus.SHIPPED, at, OrderStatus.PROCESSING);
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

  public String orderNumber() {
    return orderNumber;
  }

  public List<OrderLine> lines() {
    return List.of(line);
  }

  public ShippingAddress shippingAddress() {
    return shippingAddress;
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
}
