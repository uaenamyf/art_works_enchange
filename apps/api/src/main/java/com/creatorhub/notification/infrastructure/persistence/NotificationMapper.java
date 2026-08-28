package com.creatorhub.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 通知 SQL Mapper；user_id 条件保证列表和已读操作不能越权。 */
@Mapper
interface NotificationMapper extends BaseMapper<NotificationPersistenceRow> {

  @Insert(
      "INSERT INTO notification_events (event_id, event_type, event_version, consumed_at) "
          + "VALUES (#{eventId}, #{eventType}, #{eventVersion}, #{consumedAt}) "
          + "ON CONFLICT (event_id) DO NOTHING")
  int recordEvent(
      @Param("eventId") UUID eventId,
      @Param("eventType") String eventType,
      @Param("eventVersion") int eventVersion,
      @Param("consumedAt") Instant consumedAt);

  @Select(
      """
      <script>
      SELECT id, user_id, event_id, type, title, body, link, read_at, created_at
      FROM notifications
      WHERE user_id = #{userId}
      <if test='unreadOnly'>AND read_at IS NULL</if>
      <if test='beforeCreatedAt != null'>AND (created_at, id) &lt; (#{beforeCreatedAt}, #{beforeId})</if>
      ORDER BY created_at DESC, id DESC
      LIMIT #{limit}
      </script>
      """)
  List<NotificationPersistenceRow> findByUser(
      @Param("userId") UUID userId,
      @Param("unreadOnly") boolean unreadOnly,
      @Param("beforeCreatedAt") Instant beforeCreatedAt,
      @Param("beforeId") UUID beforeId,
      @Param("limit") int limit);

  @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND read_at IS NULL")
  int countUnread(@Param("userId") UUID userId);

  @Update(
      "UPDATE notifications SET read_at = COALESCE(read_at, #{readAt}) "
          + "WHERE id = #{notificationId} AND user_id = #{userId}")
  int markRead(
      @Param("userId") UUID userId,
      @Param("notificationId") UUID notificationId,
      @Param("readAt") Instant readAt);
}
