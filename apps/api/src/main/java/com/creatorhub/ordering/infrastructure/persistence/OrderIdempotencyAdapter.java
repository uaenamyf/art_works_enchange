package com.creatorhub.ordering.infrastructure.persistence;

import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderView;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyClaim;
import com.creatorhub.ordering.application.port.out.OrderIdempotencyPort;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 订单幂等适配器；响应从订单仓储恢复，避免在 Redis 中保存交易事实。 */
@Repository
public class OrderIdempotencyAdapter implements OrderIdempotencyPort {

  private final OrderIdempotencyMapper mapper;
  private final OrderRepository orders;

  public OrderIdempotencyAdapter(OrderIdempotencyMapper mapper, OrderRepository orders) {
    this.mapper = mapper;
    this.orders = orders;
  }

  @Override
  public OrderIdempotencyClaim claim(
      UUID buyerId, String operation, String idempotencyKey, String requestHash) {
    boolean acquired =
        mapper.claim(buyerId, operation, idempotencyKey, requestHash, java.time.Instant.now()) == 1;
    var row = mapper.find(buyerId, operation, idempotencyKey);
    if (row == null) {
      throw new IllegalStateException("Idempotency claim was not persisted");
    }
    OrderView response =
        row.getOrderId() == null
            ? null
            : orders.findById(row.getOrderId()).map(OrderViewMapper::map).orElse(null);
    return new OrderIdempotencyClaim(acquired, row.getRequestHash(), response);
  }

  @Override
  public void complete(UUID buyerId, String operation, String idempotencyKey, OrderView response) {
    if (mapper.complete(buyerId, operation, idempotencyKey, response.id()) != 1) {
      throw new IllegalStateException("Idempotency claim was already completed");
    }
  }
}
