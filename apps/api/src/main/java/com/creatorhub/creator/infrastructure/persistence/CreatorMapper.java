package com.creatorhub.creator.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Creator 档案 Mapper；写模型查询保持简单，复杂 Studio 查询后续使用专用 XML。 */
@Mapper
interface CreatorMapper extends BaseMapper<CreatorPersistenceRow> {

  /** 按创建时间和 ID 稳定倒序分页，分类筛选在数据库执行。 */
  @Select(
      """
			<script>
			SELECT id, user_id, slug, display_name, category, bio, location,
						 verified, avatar_url, cover_url, paypal_merchant_id, version, created_at, updated_at
			FROM creator_profiles
			WHERE 1 = 1
				<if test='category != null and category != ""'>AND category = #{category}</if>
				<if test='beforeCreatedAt != null'>
					AND (created_at, id) &lt; (#{beforeCreatedAt}, #{beforeId})
				</if>
			ORDER BY created_at DESC, id DESC
			LIMIT #{limit}
			</script>
			""")
  List<CreatorPersistenceRow> findPage(
      @Param("category") String category,
      @Param("beforeCreatedAt") Instant beforeCreatedAt,
      @Param("beforeId") UUID beforeId,
      @Param("limit") int limit);

  /** 显式绑定 UUID 主键，查询结果仍由下划线到驼峰配置映射为 Row。 */
  @Select(
      """
			SELECT id, user_id, slug, display_name, category, bio, location,
				   verified, avatar_url, cover_url, paypal_merchant_id, version, created_at, updated_at
			FROM creator_profiles
			WHERE id = #{id}
			""")
  CreatorPersistenceRow findByIdValue(@Param("id") UUID id);
}
