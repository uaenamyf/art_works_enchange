package com.creatorhub.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.PasswordHash;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.domain.model.UserRole;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
/**
 * 用户聚合仓储适配器，负责领域模型与 MyBatis 持久化行的显式双向转换。
 *
 * <p>本适配器只向 application 返回 {@code User} 聚合，禁止透传 {@link UserPersistenceRow}。
 */
public class UserRepositoryAdapter implements UserRepository {

  private final UserMapper mapper;

  public UserRepositoryAdapter(UserMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean existsByEmail(Email email) {
    return mapper.selectCount(new QueryWrapper<UserPersistenceRow>().eq("email", email.value()))
        > 0;
  }

  @Override
  public Optional<User> findByEmail(Email email) {
    return Optional.ofNullable(
            mapper.selectOne(
                new QueryWrapper<UserPersistenceRow>().eq("email", email.value()).last("LIMIT 1")))
        .map(UserRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<User> findById(UserId id) {
    return Optional.ofNullable(mapper.findByIdValue(id.value()))
        .map(UserRepositoryAdapter::toDomain);
  }

  @Override
  public User save(User user) {
    UserPersistenceRow row = mapper.findByIdValue(user.id().value());
    boolean inserting = row == null;
    if (inserting) {
      row = new UserPersistenceRow();
      row.setId(user.id().value());
      row.setVersion(0);
    }
    row.setEmail(user.email().value());
    row.setPasswordHash(user.passwordHash().value());
    row.setDisplayName(user.displayName());
    row.setRole(user.role().name());
    row.setDisabled(user.disabled());
    row.setCreatedAt(user.createdAt());
    row.setUpdatedAt(AppClock.now());
    if (inserting) {
      mapper.insert(row);
    } else if (mapper.updateById(row) != 1) {
      throw new IllegalStateException("User optimistic lock conflict");
    }
    return toDomain(mapper.findByIdValue(row.getId()));
  }

  private static User toDomain(UserPersistenceRow row) {
    return User.reconstitute(
        UserId.of(row.getId()),
        new Email(row.getEmail()),
        new PasswordHash(row.getPasswordHash()),
        row.getDisplayName(),
        UserRole.valueOf(row.getRole()),
        row.isDisabled(),
        row.getCreatedAt());
  }
}
