package com.creatorhub.follow.domain.repository;

import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.sharedkernel.UserId;
import java.time.Instant;
import java.util.List;

/** 关注关系仓储端口；数据库唯一键保证同一用户不能重复关注同一 Creator。 */
public interface FollowRepository {

  boolean add(UserId userId, CreatorId creatorId);

  boolean remove(UserId userId, CreatorId creatorId);

  long countByCreatorId(CreatorId creatorId);

  boolean exists(UserId userId, CreatorId creatorId);

  List<FollowEntry> findByUser(
      UserId userId, Instant beforeCreatedAt, CreatorId beforeCreatorId, int limit);

  List<FollowerEntry> findFollowers(
      CreatorId creatorId, Instant beforeCreatedAt, UserId beforeUserId, int limit);

  record FollowEntry(CreatorId creatorId, Instant createdAt) {}

  record FollowerEntry(UserId userId, Instant createdAt) {}
}
