package com.creatorhub.studio.application.port.in;

import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderPage;
import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderView;
import com.creatorhub.ordering.domain.model.OrderStatus;
import java.util.UUID;

/** Creator Studio 订单履约输入端口；状态迁移仍由 Order 聚合裁决。 */
public interface StudioOrderUseCase {

  OrderPage list(UUID sellerUserId, OrderStatus status, String cursor, int limit);

  OrderView updateStatus(
      UUID sellerUserId, UUID orderId, OrderStatus status, String trackingNumber, String carrier);
}
