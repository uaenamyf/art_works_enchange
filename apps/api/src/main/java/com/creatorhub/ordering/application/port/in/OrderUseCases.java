package com.creatorhub.ordering.application.port.in;

import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 订单公开用例；金额和商品信息由 Catalog 快照决定，客户端只能提交数量和收货地址。 */
public interface OrderUseCases {

  OrderView create(UUID buyerId, CreateCommand command, String idempotencyKey);

  OrderView cancel(UUID buyerId, UUID orderId);

  int expirePendingOrders(Instant now, int limit);

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
      Instant createdAt,
      Instant updatedAt,
      Instant expiresAt) {}

  record IdempotencyRecord(String requestHash, OrderView response) {}
}
