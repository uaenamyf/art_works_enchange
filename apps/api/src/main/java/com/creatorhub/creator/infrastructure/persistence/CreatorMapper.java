package com.creatorhub.creator.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Creator 档案 Mapper；写模型查询保持简单，复杂 Studio 查询后续使用专用 XML。 */
@Mapper
interface CreatorMapper extends BaseMapper<CreatorPersistenceRow> {

  /** 显式绑定 UUID 主键，查询结果仍由下划线到驼峰配置映射为 Row。 */
  @Select(
      """
			SELECT id, user_id, slug, display_name, category, bio, location,
				   verified, avatar_url, cover_url, version, created_at, updated_at
			FROM creator_profiles
			WHERE id = #{id}
			""")
  CreatorPersistenceRow findByIdValue(@Param("id") UUID id);
}
