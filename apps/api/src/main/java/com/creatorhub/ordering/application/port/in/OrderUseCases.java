package com.creatorhub.ordering.application.port.in;

import com.creatorhub.ordering.domain.model.FulfillmentType;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 订单公开用例；金额和商品信息由 Catalog 快照决定，客户端只能提交数量和收货地址。 */
public interface OrderUseCases {

  OrderView create(UUID buyerId, CreateCommand command, String idempotencyKey);

  OrderView cancel(UUID buyerId, UUID orderId);

  int expirePendingOrders(Instant now, int limit);

  OrderView get(UUID buyerId, UUID orderId);

  OrderPage list(UUID buyerId, OrderStatus status, String cursor, int limit);

  record CreateCommand(UUID artworkId, int quantity, ShippingAddress shippingAddress) {}

  record OrderView(
      UUID id,
      String orderNumber,
      UUID buyerId,
      UUID artworkId,
      String title,
      BigDecimal unitPrice,
      String currency,
      int quantity,
      BigDecimal subtotal,
      OrderStatus status,
      ShippingAddress shippingAddress,
      FulfillmentType fulfillmentType,
      String trackingNumber,
      String carrier,
      Instant createdAt,
      Instant updatedAt,
      Instant expiresAt) {
    public OrderView(
        UUID id,
        String orderNumber,
        UUID buyerId,
        UUID artworkId,
        String title,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal subtotal,
        OrderStatus status,
        ShippingAddress shippingAddress,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt) {
      this(
          id,
          orderNumber,
          buyerId,
          artworkId,
          title,
          unitPrice,
          currency,
          quantity,
          subtotal,
          status,
          shippingAddress,
          FulfillmentType.PHYSICAL,
          null,
          null,
          createdAt,
          updatedAt,
          expiresAt);
    }
  }

  record IdempotencyRecord(String requestHash, OrderView response) {}

  record OrderPage(List<OrderView> items, String nextCursor, boolean hasMore) {}
}
