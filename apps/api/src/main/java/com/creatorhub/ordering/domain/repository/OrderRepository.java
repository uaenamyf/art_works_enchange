package com.creatorhub.ordering.domain.repository;

import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 以订单聚合为单位定义仓储端口，应用层不感知订单表和持久化行。 */
public interface OrderRepository {

  Order save(Order order);

  Optional<Order> findById(UUID orderId);

  /** 仅允许待支付订单原子切换为已取消，返回 false 表示已被其他请求处理。 */
  boolean cancel(Order order);

  List<Order> findExpiredPending(Instant now, int limit);

  List<Order> findByBuyer(
      UUID buyerId, OrderStatus status, Instant beforeCreatedAt, UUID beforeId, int limit);

  /** 通过作品 owner 查询销售订单；查询实现位于 Ordering infrastructure。 */
  List<Order> findBySeller(
      UUID sellerUserId, OrderStatus status, Instant beforeCreatedAt, UUID beforeId, int limit);

  /** 精确校验订单是否属于当前 Creator，避免使用游标列表反查造成边界错误。 */
  Optional<Order> findByIdAndSeller(UUID orderId, UUID sellerUserId);
}
