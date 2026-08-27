package com.creatorhub.catalog.application.port.in;

import java.util.UUID;

/** Catalog 库存公开端口；Ordering 只能通过该端口预占和释放库存，不能直接访问作品表。 */
public interface StockAvailabilityPort {

  /** 使用数据库条件更新预占库存；返回 false 表示作品不存在、不可售或库存不足。 */
  boolean reserve(UUID artworkId, int quantity, UUID reservationId);

  default boolean reserve(UUID artworkId, int quantity) {
    return reserve(artworkId, quantity, UUID.randomUUID());
  }

  /** 订单取消或过期时返还已预占库存；调用方必须保证同一预占只释放一次。 */
  boolean release(UUID artworkId, int quantity, UUID reservationId);

  default void release(UUID artworkId, int quantity) {
    release(artworkId, quantity, UUID.randomUUID());
  }
}
