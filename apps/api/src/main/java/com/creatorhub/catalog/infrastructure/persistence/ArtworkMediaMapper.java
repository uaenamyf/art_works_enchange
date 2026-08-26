package com.creatorhub.catalog.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
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
}
