package com.creatorhub.ordering.application.port.out;

import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderView;
import java.util.UUID;

/** 订单幂等记录端口；生产实现必须由 PostgreSQL 唯一约束保证并发请求只有一个赢家。 */
public interface OrderIdempotencyPort {

  OrderIdempotencyClaim claim(
      UUID buyerId, String operation, String idempotencyKey, String requestHash);

  void complete(UUID buyerId, String operation, String idempotencyKey, OrderView response);
}
