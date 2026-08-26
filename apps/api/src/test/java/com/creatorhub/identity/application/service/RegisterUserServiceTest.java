package com.creatorhub.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.identity.application.port.in.RegisterUserUseCase.RegisterCommand;
import com.creatorhub.identity.application.port.out.PasswordHasher;
import com.creatorhub.identity.application.port.out.SessionIssuer;
import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.PasswordHash;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 验证注册编排：唯一邮箱、密码哈希、聚合保存和首个会话签发。 */
class RegisterUserServiceTest {

  @Test
  void registersUserWithHashedPasswordAndIssuesSession() {
    InMemoryUsers users = new InMemoryUsers();
    PasswordHasher hasher = new TestPasswordHasher();
    SessionIssuer issuer = user -> new SessionIssuer.IssuedSession("access", 900, "refresh");
    RegisterUserService service = new RegisterUserService(users, hasher, issuer);

    var result =
        service.register(new RegisterCommand("Alice@Example.com", "very-secure-password", "Alice"));

    assertThat(result.accessToken()).isEqualTo("access");
    assertThat(result.refreshToken()).isEqualTo("refresh");
    assertThat(result.role()).isEqualTo("USER");
    assertThat(users.saved)
        .singleElement()
        .satisfies(
            user -> {
              assertThat(user.email().value()).isEqualTo("alice@example.com");
              assertThat(user.passwordHash().value()).startsWith("$argon2id$");
            });
  }

  @Test
  void rejectsDuplicateEmailBeforeHashing() {
    InMemoryUsers users = new InMemoryUsers();
    users.save(
        User.register(
            com.creatorhub.sharedkernel.UserId.random(),
            new Email("alice@example.com"),
            new TestPasswordHasher().hash("very-secure-password"),
            "Alice",
            java.time.Instant.parse("2026-08-26T00:00:00Z")));
    RegisterUserService service =
        new RegisterUserService(
            users,
            new TestPasswordHasher(),
            user -> new SessionIssuer.IssuedSession("access", 900, "refresh"));

    assertThatThrownBy(
            () ->
                service.register(
                    new RegisterCommand("ALICE@example.com", "another-secure-password", "Alice 2")))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("EMAIL_TAKEN");
  }

  private static final class TestPasswordHasher implements PasswordHasher {
    @Override
    public PasswordHash hash(String plaintextPassword) {
      return new PasswordHash("$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$" + plaintextPassword);
    }

    @Override
    public boolean matches(String plaintextPassword, PasswordHash passwordHash) {
      return passwordHash.value().endsWith(plaintextPassword);
    }
  }

  private static final class InMemoryUsers implements UserRepository {
    private final List<User> saved = new ArrayList<>();

    @Override
    public boolean existsByEmail(Email email) {
      return saved.stream().anyMatch(user -> user.email().equals(email));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return saved.stream().filter(user -> user.email().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findById(UserId id) {
      return saved.stream().filter(user -> user.id().equals(id)).findFirst();
    }

    @Override
    public User save(User user) {
      saved.add(user);
      return user;
    }
  }
}
