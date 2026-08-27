package com.creatorhub.ordering.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 订单幂等键 Mapper；唯一主键由 PostgreSQL 负责并发裁决。 */
@Mapper
interface OrderIdempotencyMapper extends BaseMapper<OrderIdempotencyPersistenceRow> {

  @Select(
      "SELECT * FROM order_idempotency_keys WHERE buyer_id = #{buyerId} AND operation = #{operation} AND idempotency_key = #{idempotencyKey}")
  OrderIdempotencyPersistenceRow find(UUID buyerId, String operation, String idempotencyKey);

  @Insert(
      "INSERT INTO order_idempotency_keys (buyer_id, operation, idempotency_key, request_hash, created_at) VALUES (#{buyerId}, #{operation}, #{idempotencyKey}, #{requestHash}, #{createdAt}) ON CONFLICT (buyer_id, operation, idempotency_key) DO NOTHING")
  int claim(
      UUID buyerId, String operation, String idempotencyKey, String requestHash, Instant createdAt);

  @Update(
      "UPDATE order_idempotency_keys SET order_id = #{orderId} WHERE buyer_id = #{buyerId} AND operation = #{operation} AND idempotency_key = #{idempotencyKey} AND order_id IS NULL")
  int complete(UUID buyerId, String operation, String idempotencyKey, UUID orderId);
}
