package com.creatorhub.catalog.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/** 库存流水 Mapper；reservation_id + movement_type 唯一约束提供释放幂等性。 */
@Mapper
interface InventoryMovementMapper {

  @Insert(
      "INSERT INTO inventory_movements (id, reservation_id, artwork_id, movement_type, quantity, created_at) VALUES (#{id}, #{reservationId}, #{artworkId}, #{movementType}, #{quantity}, #{createdAt}) ON CONFLICT (reservation_id, movement_type) DO NOTHING")
  int record(
      UUID id,
      UUID reservationId,
      UUID artworkId,
      String movementType,
      int quantity,
      Instant createdAt);
}
