package com.creatorhub.follow.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.follow.application.port.in.FollowUseCases.FollowState;
import com.creatorhub.follow.domain.repository.FollowRepository;
import com.creatorhub.identity.application.port.in.GetCurrentUserUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证关注前必须确认 Creator 存在，并返回服务端计算的关注数。 */
class FollowServiceTest {

  @Test
  void followsCreatorIdempotently() {
    FollowRepository follows = mock(FollowRepository.class);
    CreatorUseCases creators = mock(CreatorUseCases.class);
    FollowService service = new FollowService(follows, creators, mock(GetCurrentUserUseCase.class));
    UUID userId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    when(follows.countByCreatorId(org.mockito.ArgumentMatchers.any())).thenReturn(3L);

    FollowState result = service.follow(userId, creatorId);

    assertThat(result).isEqualTo(new FollowState(true, 3L));
    verify(creators).get(creatorId);
    verify(follows).add(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void unfollowsCreatorIdempotently() {
    FollowRepository follows = mock(FollowRepository.class);
    CreatorUseCases creators = mock(CreatorUseCases.class);
    FollowService service = new FollowService(follows, creators, mock(GetCurrentUserUseCase.class));
    UUID userId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    when(follows.countByCreatorId(org.mockito.ArgumentMatchers.any())).thenReturn(2L);

    FollowState result = service.unfollow(userId, creatorId);

    assertThat(result).isEqualTo(new FollowState(false, 2L));
    verify(creators).get(creatorId);
    verify(follows).remove(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
