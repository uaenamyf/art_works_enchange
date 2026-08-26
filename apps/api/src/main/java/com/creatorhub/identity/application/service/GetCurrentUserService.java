package com.creatorhub.identity.application.service;

import com.creatorhub.identity.application.port.in.GetCurrentUserUseCase;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 从用户聚合仓储读取当前认证用户概要，不复用 PersistenceRow 或 JWT 中的可变资料。 */
@Service
@Transactional(readOnly = true)
public class GetCurrentUserService implements GetCurrentUserUseCase {

  private final UserRepository users;

  public GetCurrentUserService(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserSummary get(UUID userId) {
    var user =
        users
            .findById(UserId.of(userId))
            .filter(com.creatorhub.identity.domain.model.User::canIssueSession)
            .orElseThrow(() -> new BusinessError("UNAUTHORIZED", "User is unavailable"));
    return new UserSummary(user.id().value(), user.displayName(), null, user.role().name());
  }
}
