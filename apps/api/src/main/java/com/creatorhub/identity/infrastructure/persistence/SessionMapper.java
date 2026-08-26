package com.creatorhub.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Refresh 会话 Mapper；轮换查询使用 XML 中显式的 PostgreSQL {@code FOR UPDATE}。 */
@Mapper
interface SessionMapper extends BaseMapper<SessionPersistenceRow> {

  SessionPersistenceRow findByRefreshTokenHashForUpdate(
      @Param("refreshTokenHash") String refreshTokenHash);

  /** 按 UUID 主键查询会话，供幂等撤销使用。 */
  @Select(
      """
            SELECT id, user_id, refresh_token_hash, expires_at, revoked_at, created_at
            FROM identity_sessions
            WHERE id = #{id}
            """)
  SessionPersistenceRow findByIdValue(@Param("id") UUID id);
}
