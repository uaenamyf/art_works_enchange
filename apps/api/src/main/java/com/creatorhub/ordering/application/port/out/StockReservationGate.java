package com.creatorhub.ordering.application.port.out;

import java.util.UUID;

/** Redis 库存快速闸门；故障时允许回源 PostgreSQL，不能把缓存当作库存事实源。 */
public interface StockReservationGate {

  Reservation reserve(UUID artworkId, int quantity);

  void release(Reservation reservation);

  record Reservation(UUID artworkId, int quantity, boolean allowed, boolean cached) {}
}
