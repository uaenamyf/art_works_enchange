package com.creatorhub.studio.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** Studio Dashboard 只读 SQL；通过 owner_id 聚合，不向 application 层泄露数据库行。 */
@Mapper
interface StudioDashboardMapper {

  @Select(
      """
      SELECT COALESCE(SUM(CASE WHEN o.status IN ('PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED') THEN o.subtotal ELSE 0 END), 0) AS revenue,
             COALESCE(MAX(o.currency), 'CNY') AS currency,
             COUNT(DISTINCT CASE WHEN o.status IN ('PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED') THEN o.id END) AS order_count,
             (SELECT COUNT(*) FROM artworks a WHERE a.owner_id = #{userId} AND a.status = 'PUBLISHED') AS published_artwork_count,
                 (SELECT COUNT(*) FROM creator_follows f JOIN creator_profiles c ON c.id = f.creator_id WHERE c.user_id = #{userId}) AS follower_count,
                 (SELECT COUNT(*) FROM digital_download_grants g
                     JOIN digital_entitlements e ON e.id = g.entitlement_id
                     JOIN artworks da ON da.id = e.artwork_id
                                WHERE da.owner_id = #{userId}
                                    AND da.product_type = 'DIGITAL'
                                    AND g.used_at IS NOT NULL) AS digital_download_count
      FROM orders o
      JOIN artworks a ON a.id = o.artwork_id
      WHERE a.owner_id = #{userId}
      """)
  StudioDashboardRow findByUserId(UUID userId);

  record StudioDashboardRow(
      BigDecimal revenue,
      String currency,
      long orderCount,
      long publishedArtworkCount,
      long followerCount,
      long digitalDownloadCount) {}
}
