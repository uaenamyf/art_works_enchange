package com.creatorhub.ordering.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.creatorhub.ordering.domain.model.OrderStatus;
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
        INSERT INTO orders (id, order_number, buyer_id, seller_id, artwork_id, title, unit_price, currency,
            quantity, subtotal, status, fulfillment_type, shipping_address, tracking_number, carrier, expires_at, created_at, updated_at)
        VALUES (#{id}, #{orderNumber}, #{buyerId}, #{sellerId}, #{artworkId}, #{title}, #{unitPrice}, #{currency},
            #{quantity}, #{subtotal}, #{status}, #{fulfillmentType}, CAST(#{shippingAddress} AS jsonb), #{trackingNumber}, #{carrier}, #{expiresAt}, #{createdAt}, #{updatedAt})
      """)
  int insertOrder(OrderPersistenceRow row);

  @Select("SELECT * FROM orders WHERE id = #{id}")
  OrderPersistenceRow findByIdValue(UUID id);

  @Update(
      "UPDATE orders SET status = #{status}, updated_at = #{updatedAt} WHERE id = #{id} AND status = 'PENDING_PAYMENT'")
  int cancel(OrderPersistenceRow row);

  /** 仅持久化聚合状态变化；状态合法性由领域聚合负责，数据库按订单 ID 原子更新。 */
  @Update(
      "UPDATE orders SET status = #{status}, tracking_number = #{trackingNumber}, carrier = #{carrier}, updated_at = #{updatedAt} WHERE id = #{id}")
  int updateStatus(OrderPersistenceRow row);

  @Select(
      "SELECT * FROM orders WHERE status = 'PENDING_PAYMENT' AND expires_at <= #{now} ORDER BY expires_at ASC, id ASC LIMIT #{limit}")
  List<OrderPersistenceRow> findExpiredPending(Instant now, int limit);

  @Select(
      """
            <script>
            SELECT * FROM orders
            WHERE buyer_id = #{buyerId}
                <if test='status != null'>AND status = #{status}</if>
                <if test='beforeCreatedAt != null'>AND (created_at, id) &lt; (#{beforeCreatedAt}, #{beforeId})</if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            </script>
            """)
  List<OrderPersistenceRow> findByBuyer(
      UUID buyerId, OrderStatus status, Instant beforeCreatedAt, UUID beforeId, int limit);

  /** 销售订单只通过不可变 seller_id 快照隔离 Creator owner，避免跨上下文表 Join。 */
  @Select(
      """
            <script>
            SELECT o.*
            FROM orders o
            WHERE o.seller_id = #{sellerUserId}
                <if test='status != null'>AND o.status = #{status}</if>
                <if test='beforeCreatedAt != null'>AND (o.created_at, o.id) &lt; (#{beforeCreatedAt}, #{beforeId})</if>
            ORDER BY o.created_at DESC, o.id DESC
            LIMIT #{limit}
            </script>
            """)
  List<OrderPersistenceRow> findBySeller(
      UUID sellerUserId, OrderStatus status, Instant beforeCreatedAt, UUID beforeId, int limit);

  @Select("SELECT * FROM orders WHERE id = #{orderId} AND seller_id = #{sellerUserId}")
  OrderPersistenceRow findByIdAndSeller(UUID orderId, UUID sellerUserId);
}
