package com.creatorhub.identity.domain.repository;

import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.sharedkernel.UserId;
import java.util.Optional;

/**
 * 以 {@link User} 聚合根为单位定义的领域仓储端口。
 *
 * <p>邮箱查询只接受已规范化的 {@link Email}，实现必须返回领域聚合而不是 PersistenceRow。
 */
public interface UserRepository {

  boolean existsByEmail(Email email);

  Optional<User> findByEmail(Email email);

  Optional<User> findById(UserId id);

  User save(User user);
}
