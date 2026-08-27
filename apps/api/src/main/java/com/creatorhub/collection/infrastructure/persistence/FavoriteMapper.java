package com.creatorhub.collection.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 收藏关系 Mapper；INSERT ON CONFLICT 和 DELETE 共同提供重复操作幂等性。 */
@Mapper
interface FavoriteMapper extends BaseMapper<FavoritePersistenceRow> {

  /** 按用户和收藏时间稳定倒序分页，游标使用创建时间和作品 ID。 */
  @Select(
      """
            <script>
            SELECT artwork_id, created_at
            FROM favorite_collections
            WHERE user_id = #{userId}
                <if test='beforeCreatedAt != null'>
                    AND (created_at, artwork_id) &lt; (#{beforeCreatedAt}, #{beforeArtworkId})
                </if>
            ORDER BY created_at DESC, artwork_id DESC
            LIMIT #{limit}
            </script>
            """)
  List<FavoritePersistenceRow> findPage(
      @Param("userId") UUID userId,
      @Param("beforeCreatedAt") Instant beforeCreatedAt,
      @Param("beforeArtworkId") UUID beforeArtworkId,
      @Param("limit") int limit);

  @Insert(
      """
      INSERT INTO favorite_collections (user_id, artwork_id, created_at)
      VALUES (#{userId}, #{artworkId}, #{createdAt})
      ON CONFLICT (user_id, artwork_id) DO NOTHING
      """)
  int add(
      @Param("userId") UUID userId,
      @Param("artworkId") UUID artworkId,
      @Param("createdAt") Instant createdAt);

  @Delete(
      "DELETE FROM favorite_collections WHERE user_id = #{userId} AND artwork_id = #{artworkId}")
  int remove(@Param("userId") UUID userId, @Param("artworkId") UUID artworkId);

  @Select("SELECT COUNT(*) FROM favorite_collections WHERE artwork_id = #{artworkId}")
  long countByArtworkId(@Param("artworkId") UUID artworkId);

  @Select(
      "SELECT COUNT(*) FROM favorite_collections WHERE user_id = #{userId} AND artwork_id = #{artworkId}")
  long exists(@Param("userId") UUID userId, @Param("artworkId") UUID artworkId);
}
