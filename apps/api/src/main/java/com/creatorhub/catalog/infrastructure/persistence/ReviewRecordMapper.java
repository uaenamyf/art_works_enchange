package com.creatorhub.catalog.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 审核记录 Mapper；记录采用追加写入，不允许修改历史审核结论。 */
@Mapper
interface ReviewRecordMapper extends BaseMapper<ReviewRecordPersistenceRow> {

  @Select(
      """
			SELECT id, artwork_id, reviewer_id, decision, reason, created_at
			FROM artwork_review_records
			WHERE artwork_id = #{artworkId}
			ORDER BY created_at DESC, id DESC
			""")
  List<ReviewRecordPersistenceRow> findByArtworkId(@Param("artworkId") UUID artworkId);
}
