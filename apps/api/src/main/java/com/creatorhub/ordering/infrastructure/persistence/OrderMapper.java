package com.creatorhub.ordering.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 订单持久化 Mapper；订单写入与幂等键由同一 PostgreSQL 事务协调。 */
@Mapper
interface OrderMapper extends BaseMapper<OrderPersistenceRow> {

  @Insert(
      """
      INSERT INTO orders (id, order_number, buyer_id, artwork_id, title, unit_price, currency,
          quantity, subtotal, status, shipping_address, expires_at, created_at, updated_at)
      VALUES (#{id}, #{orderNumber}, #{buyerId}, #{artworkId}, #{title}, #{unitPrice}, #{currency},
          #{quantity}, #{subtotal}, #{status}, CAST(#{shippingAddress} AS jsonb), #{expiresAt}, #{createdAt}, #{updatedAt})
      """)
  int insertOrder(OrderPersistenceRow row);

  @Select("SELECT * FROM orders WHERE id = #{id}")
  OrderPersistenceRow findByIdValue(UUID id);

  @Update(
      "UPDATE orders SET status = #{status}, updated_at = #{updatedAt} WHERE id = #{id} AND status = 'PENDING_PAYMENT'")
  int cancel(OrderPersistenceRow row);

  @Select(
      "SELECT * FROM orders WHERE status = 'PENDING_PAYMENT' AND expires_at <= #{now} ORDER BY expires_at ASC, id ASC LIMIT #{limit}")
  List<OrderPersistenceRow> findExpiredPending(Instant now, int limit);
}
