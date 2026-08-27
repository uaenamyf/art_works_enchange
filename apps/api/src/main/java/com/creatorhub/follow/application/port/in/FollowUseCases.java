package com.creatorhub.follow.application.port.in;

import com.creatorhub.creator.application.port.in.CreatorUseCases.CreatorSummaryView;
import com.creatorhub.identity.application.port.in.GetCurrentUserUseCase.UserSummary;
import java.util.List;
import java.util.UUID;

/** 关注公开用例；用户身份来自认证上下文，Creator ID 只用于定位公开 Creator。 */
public interface FollowUseCases {

  FollowState follow(UUID userId, UUID creatorId);

  FollowState unfollow(UUID userId, UUID creatorId);

  CreatorPage listMyFollows(UUID userId, String cursor, int limit);

  FollowerPage listFollowers(UUID creatorId, String cursor, int limit);

  interface ReadPort {
    long countByCreatorId(UUID creatorId);

    boolean isFollowing(UUID userId, UUID creatorId);
  }

  record FollowState(boolean following, long followerCount) {}

  record CreatorPage(List<CreatorSummaryView> items, String nextCursor, boolean hasMore) {}

  record FollowerPage(List<UserSummary> items, String nextCursor, boolean hasMore) {}
}
