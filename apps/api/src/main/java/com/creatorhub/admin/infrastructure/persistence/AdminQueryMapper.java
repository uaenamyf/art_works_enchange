package com.creatorhub.admin.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Admin 只读报表 SQL；固定过滤审核状态，避免后台查询意外暴露未提交草稿。 */
@Mapper
interface AdminQueryMapper {

  @Select(
      """
            <script>
      SELECT a.id, a.title, a.description, a.category, a.product_type,
             a.price_amount, a.price_currency, a.stock, a.status, a.updated_at,
             c.id AS creator_id, c.slug AS creator_slug,
             c.display_name AS creator_display_name, c.category AS creator_category
      FROM artworks a
      JOIN creator_profiles c ON c.id = a.creator_id
      WHERE a.status = 'PENDING_REVIEW'
                <if test='beforeUpdatedAt != null'>
                    AND (a.updated_at, a.id) &lt; (#{beforeUpdatedAt}, #{beforeId})
                </if>
      ORDER BY a.updated_at DESC, a.id DESC
      LIMIT #{limit}
            </script>
      """)
  List<ReviewQueueRow> findReviewQueue(
      @Param("beforeUpdatedAt") Instant beforeUpdatedAt,
      @Param("beforeId") UUID beforeId,
      @Param("limit") int limit);

  @Select(
      """
        SELECT a.id, a.title, a.description, a.category, a.product_type,
           a.price_amount, a.price_currency, a.stock, a.status, a.updated_at,
           c.id AS creator_id, c.slug AS creator_slug,
           c.display_name AS creator_display_name, c.category AS creator_category
        FROM artworks a
        JOIN creator_profiles c ON c.id = a.creator_id
        WHERE a.id = #{artworkId} AND a.status = 'PENDING_REVIEW'
        """)
  ReviewQueueRow findReviewArtwork(@Param("artworkId") UUID artworkId);

  @Select(
      """
                <script>
                SELECT r.id, r.artwork_id, a.title AS artwork_title, r.reviewer_id,
                             r.decision, r.reason, r.created_at,
                             c.id AS creator_id, c.slug AS creator_slug,
                             c.display_name AS creator_display_name, c.category AS creator_category
                FROM artwork_review_records r
                JOIN artworks a ON a.id = r.artwork_id
                JOIN creator_profiles c ON c.id = a.creator_id
                <if test='beforeCreatedAt != null'>
                    WHERE (r.created_at, r.id) &lt; (#{beforeCreatedAt}, #{beforeId})
                </if>
                <if test='beforeCreatedAt == null'>
                    WHERE TRUE
                </if>
                ORDER BY r.created_at DESC, r.id DESC
                LIMIT #{limit}
                </script>
                """)
  List<ReviewHistoryRow> findReviewHistory(
      @Param("beforeCreatedAt") Instant beforeCreatedAt,
      @Param("beforeId") UUID beforeId,
      @Param("limit") int limit);

  @Select(
      """
      SELECT
        (SELECT COUNT(*) FROM users) AS user_count,
        (SELECT COUNT(*) FROM creator_profiles) AS creator_count,
        (SELECT COUNT(*) FROM artworks WHERE status = 'PUBLISHED') AS published_artwork_count,
        (SELECT COUNT(*) FROM artworks WHERE status = 'PENDING_REVIEW') AS pending_review_artwork_count
      """)
  DashboardTotalsRow findDashboardTotals();

  @Select(
      """
      SELECT status, COUNT(*) AS count
      FROM artworks
      GROUP BY status
      ORDER BY status
      """)
  List<StatusCountRow> countArtworksByStatus();

  @Select(
      """
      SELECT status, COUNT(*) AS count
      FROM orders
      GROUP BY status
      ORDER BY status
      """)
  List<StatusCountRow> countOrdersByStatus();

  @Select(
      """
            SELECT id
            FROM artwork_media
            WHERE artwork_id = #{artworkId}
            ORDER BY sort_order ASC, id ASC
            """)
  List<UUID> findMediaIds(@Param("artworkId") UUID artworkId);

  record ReviewQueueRow(
      UUID id,
      String title,
      String description,
      String category,
      String productType,
      BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      String status,
      Instant updatedAt,
      UUID creatorId,
      String creatorSlug,
      String creatorDisplayName,
      String creatorCategory) {}

  record ReviewHistoryRow(
      UUID id,
      UUID artworkId,
      String artworkTitle,
      UUID reviewerId,
      String decision,
      String reason,
      Instant createdAt,
      UUID creatorId,
      String creatorSlug,
      String creatorDisplayName,
      String creatorCategory) {}

  record DashboardTotalsRow(
      long userCount,
      long creatorCount,
      long publishedArtworkCount,
      long pendingReviewArtworkCount) {}

  record StatusCountRow(String status, long count) {}
}
