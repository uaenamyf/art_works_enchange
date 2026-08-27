package com.creatorhub.follow.infrastructure.persistence;

import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.follow.application.port.in.FollowUseCases;
import com.creatorhub.follow.domain.repository.FollowRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 关注仓储适配器；只在 Follow 边界内转换强类型 ID 和数据库 UUID。 */
@Repository
public class FollowRepositoryAdapter implements FollowRepository, FollowUseCases.ReadPort {

  private final FollowMapper mapper;

  public FollowRepositoryAdapter(FollowMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean add(UserId userId, CreatorId creatorId) {
    return mapper.add(userId.value(), creatorId.value(), AppClock.now()) == 1;
  }

  @Override
  public boolean remove(UserId userId, CreatorId creatorId) {
    return mapper.remove(userId.value(), creatorId.value()) == 1;
  }

  @Override
  public long countByCreatorId(CreatorId creatorId) {
    return mapper.countByCreatorId(creatorId.value());
  }

  @Override
  public long countByCreatorId(UUID creatorId) {
    return countByCreatorId(CreatorId.of(creatorId));
  }

  @Override
  public boolean isFollowing(UUID userId, UUID creatorId) {
    return exists(UserId.of(userId), CreatorId.of(creatorId));
  }

  @Override
  public boolean exists(UserId userId, CreatorId creatorId) {
    return mapper.exists(userId.value(), creatorId.value()) > 0;
  }

  @Override
  public List<FollowEntry> findByUser(
      UserId userId, Instant beforeCreatedAt, CreatorId beforeCreatorId, int limit) {
    return mapper
        .findByUser(
            userId.value(),
            beforeCreatedAt,
            beforeCreatorId == null ? null : beforeCreatorId.value(),
            limit)
        .stream()
        .map(row -> new FollowEntry(CreatorId.of(row.getCreatorId()), row.getCreatedAt()))
        .toList();
  }

  @Override
  public List<FollowerEntry> findFollowers(
      CreatorId creatorId, Instant beforeCreatedAt, UserId beforeUserId, int limit) {
    return mapper
        .findFollowers(
            creatorId.value(),
            beforeCreatedAt,
            beforeUserId == null ? null : beforeUserId.value(),
            limit)
        .stream()
        .map(row -> new FollowerEntry(UserId.of(row.getUserId()), row.getCreatedAt()))
        .toList();
  }
}
