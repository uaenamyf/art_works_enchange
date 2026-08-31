package com.creatorhub.studio.application.service;

import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderPage;
import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderView;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import com.creatorhub.studio.application.port.in.StudioOrderUseCase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Studio 订单应用服务；按作品 owner 做对象级隔离，状态变更交给 Order 聚合。 */
@Service
@Transactional
public class StudioOrderService implements StudioOrderUseCase {

  private final OrderRepository orders;
  private final OutboxRepository outbox;

  public StudioOrderService(OrderRepository orders) {
    this(orders, null);
  }

  @Autowired
  public StudioOrderService(OrderRepository orders, OutboxRepository outbox) {
    this.orders = orders;
    this.outbox = outbox;
  }

  @Override
  @Transactional(readOnly = true)
  public OrderPage list(UUID sellerUserId, OrderStatus status, String cursor, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decode(cursor);
    List<Order> results =
        orders.findBySeller(sellerUserId, status, decoded.createdAt(), decoded.id(), pageSize + 1);
    boolean hasMore = results.size() > pageSize;
    List<Order> page = results.stream().limit(pageSize).toList();
    return new OrderPage(
        page.stream().map(StudioOrderService::view).toList(),
        hasMore ? Cursor.encode(page.get(page.size() - 1)) : null,
        hasMore);
  }

  @Override
  public OrderView updateStatus(
      UUID sellerUserId, UUID orderId, OrderStatus status, String trackingNumber, String carrier) {
    Order order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new BusinessError("ORDER_NOT_FOUND", "Order not found"));
    if (orders.findByIdAndSeller(orderId, sellerUserId).isEmpty()) {
      throw new BusinessError("ORDER_FORBIDDEN", "Order does not belong to creator");
    }
    OrderStatus previousStatus = order.status();
    if (status == OrderStatus.PROCESSING) order.startProcessing(Instant.now());
    else if (status == OrderStatus.SHIPPED)
      order.markShipped(Instant.now(), trackingNumber, carrier);
    else throw new BusinessError("ORDER_STATUS_INVALID", "Unsupported fulfillment status");
    orders.save(order);
    appendStatusChanged(order, previousStatus);
    return view(order);
  }

  /** 履约状态落库后写入 Outbox，避免通知发送失败影响订单事务。 */
  private void appendStatusChanged(Order order, OrderStatus previousStatus) {
    if (outbox == null) return;
    String body = order.status() == OrderStatus.PROCESSING ? "创作者已开始处理你的订单。" : "你的订单已发货，请留意物流信息。";
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
                "body", body)));
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

  private record Cursor(Instant createdAt, UUID id) {
    static Cursor decode(String value) {
      if (value == null || value.isBlank()) return new Cursor(null, null);
      try {
        String[] parts =
            new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                .split("\\|", 2);
        return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
      } catch (RuntimeException exception) {
        throw new BusinessError("INVALID_CURSOR", "Cursor is invalid");
      }
    }

    static String encode(Order order) {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              (order.createdAt() + "|" + order.id().value()).getBytes(StandardCharsets.UTF_8));
    }
  }
}
