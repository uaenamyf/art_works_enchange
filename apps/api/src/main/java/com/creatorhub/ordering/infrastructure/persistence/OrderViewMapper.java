package com.creatorhub.ordering.infrastructure.persistence;

import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderView;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderLine;

/** 将订单聚合转换为幂等重放和 HTTP 查询共用的只读响应快照。 */
final class OrderViewMapper {

  private OrderViewMapper() {}

  static OrderView map(Order order) {
    OrderLine line = order.lines().get(0);
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
}
