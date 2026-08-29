package com.creatorhub.catalog.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** Artwork 主表 Mapper；UUID 主键查询显式声明，避免 MyBatis-Plus 属性推断问题。 */
@Mapper
interface ArtworkMapper extends BaseMapper<ArtworkPersistenceRow> {

  /** 数据库最终裁决库存，条件不满足时不会修改任何行。 */
  @Update(
      """
            UPDATE artworks
            SET stock = stock - #{quantity}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{artworkId} AND status = 'PUBLISHED' AND stock >= #{quantity}
            """)
  int reserveStock(@Param("artworkId") UUID artworkId, @Param("quantity") int quantity);

  /** 返还库存只由 Ordering 的取消/过期流程调用，并在应用层保证幂等。 */
  @Update(
      """
            UPDATE artworks
            SET stock = stock + #{quantity}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{artworkId}
            """)
  int releaseStock(@Param("artworkId") UUID artworkId, @Param("quantity") int quantity);

  @Select("SELECT stock FROM artworks WHERE id = #{artworkId} AND status = 'PUBLISHED'")
  Integer findPublishedStock(@Param("artworkId") UUID artworkId);

  /** 按 created_at/id 的稳定倒序游标查询公开作品；关键词和分类均在数据库过滤。 */
  @Select(
      """
            <script>
            SELECT id, owner_id, creator_id, title, description, category,
                         price_amount, price_currency, stock, status, version,
                         review_reason, reviewed_at, created_at, updated_at
            FROM artworks
            WHERE status = 'PUBLISHED'
                <if test='query != null and query != ""'>
                    AND (title ILIKE CONCAT('%', #{query}, '%') OR description ILIKE CONCAT('%', #{query}, '%'))
                </if>
                <if test='category != null and category != ""'>AND category = #{category}</if>
                <if test='creatorId != null'>AND creator_id = #{creatorId}</if>
                <if test='beforeCreatedAt != null'>
                    AND (created_at, id) &lt; (#{beforeCreatedAt}, #{beforeId})
                </if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            </script>
            """)
  List<ArtworkPersistenceRow> findPublishedPage(
      @Param("query") String query,
      @Param("category") String category,
      @Param("creatorId") UUID creatorId,
      @Param("beforeCreatedAt") Instant beforeCreatedAt,
      @Param("beforeId") UUID beforeId,
      @Param("limit") int limit);

  /** Studio 作品列表按 owner 隔离草稿和审核状态，游标按更新时间稳定分页。 */
  @Select(
      """
            <script>
            SELECT id, owner_id, creator_id, title, description, category,
                         price_amount, price_currency, stock, product_type, status, version,
                         review_reason, reviewed_at, created_at, updated_at
            FROM artworks
            WHERE owner_id = #{ownerId}
                <if test='status != null'>AND status = #{status}</if>
                <if test='beforeUpdatedAt != null'>AND (updated_at, id) &lt; (#{beforeUpdatedAt}, #{beforeId})</if>
            ORDER BY updated_at DESC, id DESC
            LIMIT #{limit}
            </script>
            """)
  List<ArtworkPersistenceRow> findOwnedPage(
      @Param("ownerId") UUID ownerId,
      @Param("status") ArtworkStatus status,
      @Param("beforeUpdatedAt") Instant beforeUpdatedAt,
      @Param("beforeId") UUID beforeId,
      @Param("limit") int limit);

  /** 查询指定 owner 的作品，用于对象级权限校验。 */
  @Select(
      """
      SELECT id, owner_id, creator_id, title, description, category,
             price_amount, price_currency, stock, status, version,
             review_reason, reviewed_at, created_at, updated_at
      FROM artworks
      WHERE id = #{id} AND owner_id = #{ownerId}
      """)
  ArtworkPersistenceRow findByOwnerId(@Param("ownerId") UUID ownerId, @Param("id") UUID id);

  /** 显式 UUID 主键查询，状态转换由 EnumTypeHandler 负责。 */
  @Select(
      """
      SELECT id, owner_id, creator_id, title, description, category,
             price_amount, price_currency, stock, status, version,
             review_reason, reviewed_at, created_at, updated_at
      FROM artworks
      WHERE id = #{id}
      """)
  ArtworkPersistenceRow findByIdValue(@Param("id") UUID id);

  /** 公开查询在 SQL 层过滤状态，避免调用方遗漏审核门禁。 */
  @Select(
      """
      SELECT id, owner_id, creator_id, title, description, category,
             price_amount, price_currency, stock, status, version,
             review_reason, reviewed_at, created_at, updated_at
      FROM artworks
      WHERE id = #{id} AND status = 'PUBLISHED'
      """)
  ArtworkPersistenceRow findPublishedById(@Param("id") UUID id);
}
