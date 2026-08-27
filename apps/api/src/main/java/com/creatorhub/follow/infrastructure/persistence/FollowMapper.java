package com.creatorhub.follow.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 关注关系 Mapper；INSERT ON CONFLICT 和 DELETE 共同提供重复操作幂等性。 */
@Mapper
interface FollowMapper extends BaseMapper<FollowPersistenceRow> {

  @Select(
      """
    <script>
    SELECT creator_id, created_at FROM creator_follows
    WHERE user_id = #{userId}
    <if test='beforeCreatedAt != null'>AND (created_at, creator_id) &lt; (#{beforeCreatedAt}, #{beforeCreatorId})</if>
    ORDER BY created_at DESC, creator_id DESC LIMIT #{limit}
    </script>
    """)
  List<FollowPersistenceRow> findByUser(
      @Param("userId") UUID userId,
      @Param("beforeCreatedAt") Instant beforeCreatedAt,
      @Param("beforeCreatorId") UUID beforeCreatorId,
      @Param("limit") int limit);

  @Select(
      """
    <script>
    SELECT user_id, created_at FROM creator_follows
    WHERE creator_id = #{creatorId}
    <if test='beforeCreatedAt != null'>AND (created_at, user_id) &lt; (#{beforeCreatedAt}, #{beforeUserId})</if>
    ORDER BY created_at DESC, user_id DESC LIMIT #{limit}
    </script>
    """)
  List<FollowPersistenceRow> findFollowers(
      @Param("creatorId") UUID creatorId,
      @Param("beforeCreatedAt") Instant beforeCreatedAt,
      @Param("beforeUserId") UUID beforeUserId,
      @Param("limit") int limit);

  @Insert(
      """
      INSERT INTO creator_follows (user_id, creator_id, created_at)
      VALUES (#{userId}, #{creatorId}, #{createdAt})
      ON CONFLICT (user_id, creator_id) DO NOTHING
      """)
  int add(
      @Param("userId") UUID userId,
      @Param("creatorId") UUID creatorId,
      @Param("createdAt") Instant createdAt);

  @Delete("DELETE FROM creator_follows WHERE user_id = #{userId} AND creator_id = #{creatorId}")
  int remove(@Param("userId") UUID userId, @Param("creatorId") UUID creatorId);

  @Select("SELECT COUNT(*) FROM creator_follows WHERE creator_id = #{creatorId}")
  long countByCreatorId(@Param("creatorId") UUID creatorId);

  @Select(
      "SELECT COUNT(*) FROM creator_follows WHERE user_id = #{userId} AND creator_id = #{creatorId}")
  long exists(@Param("userId") UUID userId, @Param("creatorId") UUID creatorId);
}
