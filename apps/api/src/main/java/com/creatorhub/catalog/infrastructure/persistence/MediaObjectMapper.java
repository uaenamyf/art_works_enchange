package com.creatorhub.catalog.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 媒体对象 Mapper；状态迁移使用带 owner 和状态条件的显式 SQL。 */
@Mapper
interface MediaObjectMapper extends BaseMapper<MediaObjectPersistenceRow> {

  /** 只有绑定到已发布作品的 READY 媒体才允许生成公开读取地址。 */
  @Select(
      """
      SELECT mo.id, mo.object_key, mo.content_type
      FROM media_objects mo
      JOIN artwork_media am ON am.id = mo.id
      JOIN artworks a ON a.id = am.artwork_id
      WHERE mo.id = #{mediaId}
        AND mo.upload_status = 'READY'
        AND a.status = 'PUBLISHED'
      """)
  MediaPublicPersistenceRow findPublished(@Param("mediaId") UUID mediaId);

  /** 只返回当前用户仍可确认的待上传对象 key。 */
  @Select(
      """
      SELECT object_key
      FROM media_objects
      WHERE id = #{mediaId}
        AND owner_id = #{ownerId}
        AND upload_status = 'PENDING'
        AND expires_at > CURRENT_TIMESTAMP
      """)
  String findPendingObjectKey(@Param("mediaId") UUID mediaId, @Param("ownerId") UUID ownerId);

  /** 原子地把当前用户的 PENDING 媒体标记为 READY，防止越权或重复确认。 */
  @Update(
      """
      UPDATE media_objects
      SET upload_status = 'READY', uploaded_at = #{uploadedAt}
      WHERE id = #{mediaId}
        AND owner_id = #{ownerId}
        AND upload_status = 'PENDING'
        AND expires_at > CURRENT_TIMESTAMP
      """)
  int markReady(
      @Param("mediaId") UUID mediaId,
      @Param("ownerId") UUID ownerId,
      @Param("uploadedAt") Instant uploadedAt);
}
