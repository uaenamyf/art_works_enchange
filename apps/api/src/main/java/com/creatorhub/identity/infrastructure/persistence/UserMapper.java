package com.creatorhub.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Identity 用户表 Mapper；复杂交易 SQL 不应放入 BaseMapper 默认方法。 */
@Mapper
interface UserMapper extends BaseMapper<UserPersistenceRow> {

  /** 显式绑定 UUID 主键，避免 BaseMapper 在部分版本中把 UUID 当作复合对象解析。 */
  @Select(
      """
			SELECT id, email, password_hash, display_name, avatar_url,
				   role, disabled, version, created_at, updated_at
			FROM users
			WHERE id = #{id}
			""")
  UserPersistenceRow findByIdValue(@Param("id") UUID id);
}
