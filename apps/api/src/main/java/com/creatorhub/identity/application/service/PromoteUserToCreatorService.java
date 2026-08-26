package com.creatorhub.identity.application.service;

import com.creatorhub.identity.application.port.in.PromoteUserToCreatorUseCase;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Identity 公开用例：在 Creator 申请成功后幂等提升用户角色。 */
@Service
@Transactional
public class PromoteUserToCreatorService implements PromoteUserToCreatorUseCase {

  private final UserRepository users;

  public PromoteUserToCreatorService(UserRepository users) {
    this.users = users;
  }

  @Override
  public void promote(UUID userId) {
    var user =
        users
            .findById(UserId.of(userId))
            .orElseThrow(() -> new BusinessError("UNAUTHORIZED", "User is unavailable"));
    user.promoteToCreator();
    users.save(user);
  }
}
