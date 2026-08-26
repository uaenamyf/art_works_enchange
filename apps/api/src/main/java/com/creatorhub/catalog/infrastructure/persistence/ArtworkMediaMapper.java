package com.creatorhub.catalog.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Artwork 媒体关联查询 Mapper；媒体必须按作品归属读取，禁止跨作品复用对象。 */
@Mapper
interface ArtworkMediaMapper {

  /** 按稳定排序返回作品已绑定的媒体 ID，供 Artwork 聚合恢复使用。 */
  @Select(
      """
      SELECT id
      FROM artwork_media
      WHERE artwork_id = #{artworkId}
      ORDER BY sort_order ASC, id ASC
      """)
  List<UUID> findIdsByArtworkId(@Param("artworkId") UUID artworkId);

  /** 先删除旧绑定，再按聚合当前媒体集合重建关联，避免客户端绕过聚合追加媒体。 */
  @Delete("DELETE FROM artwork_media WHERE artwork_id = #{artworkId}")
  int deleteByArtworkId(@Param("artworkId") UUID artworkId);

  /** 媒体对象已经由上传流程创建；这里仅建立作品与媒体的关联。 */
  @Insert(
      """
      INSERT INTO artwork_media (id, artwork_id, object_key, content_type, size_bytes, sort_order, uploaded_at)
      SELECT id, #{artworkId}, object_key, content_type, size_bytes, #{sortOrder}, uploaded_at
      FROM media_objects
      WHERE id = #{mediaId} AND owner_id = #{ownerId} AND upload_status = 'READY'
      """)
  int bindReadyMedia(
      @Param("artworkId") UUID artworkId,
      @Param("mediaId") UUID mediaId,
      @Param("ownerId") UUID ownerId,
      @Param("sortOrder") int sortOrder);
}
