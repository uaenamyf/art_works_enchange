package com.creatorhub.catalog.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Artwork 主表 Mapper；UUID 主键查询显式声明，避免 MyBatis-Plus 属性推断问题。 */
@Mapper
interface ArtworkMapper extends BaseMapper<ArtworkPersistenceRow> {

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
