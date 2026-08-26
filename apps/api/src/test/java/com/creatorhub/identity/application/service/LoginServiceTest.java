package com.creatorhub.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.identity.application.port.in.LoginUseCase.LoginCommand;
import com.creatorhub.identity.application.port.out.PasswordHasher;
import com.creatorhub.identity.application.port.out.SessionIssuer;
import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.PasswordHash;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 验证登录成功路径以及账户枚举防护：错误密码与禁用账号返回同一公开错误。 */
class LoginServiceTest {

  @Test
  void authenticatesActiveUserAndIssuesSession() {
    User user = activeUser();
    LoginService service =
        new LoginService(
            new SingleUserRepository(user),
            matchingHasher(),
            found -> new SessionIssuer.IssuedSession("access", 900, "refresh"));

    var result = service.login(new LoginCommand("ALICE@example.com", "very-secure-password"));

    assertThat(result.accessToken()).isEqualTo("access");
    assertThat(result.userId()).isEqualTo(user.id().value());
  }

  @Test
  void rejectsWrongPasswordAndDisabledUserWithSamePublicError() {
    User active = activeUser();
    LoginService wrongPassword =
        new LoginService(
            new SingleUserRepository(active),
            rejectingHasher(),
            found -> new SessionIssuer.IssuedSession("access", 900, "refresh"));
    assertInvalidCredentials(
        () -> wrongPassword.login(new LoginCommand("alice@example.com", "wrong-password")));

    active.disable();
    LoginService disabled =
        new LoginService(
            new SingleUserRepository(active),
            matchingHasher(),
            found -> new SessionIssuer.IssuedSession("access", 900, "refresh"));
    assertInvalidCredentials(
        () -> disabled.login(new LoginCommand("alice@example.com", "very-secure-password")));
  }

  private static void assertInvalidCredentials(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("INVALID_CREDENTIALS");
  }

  private static User activeUser() {
    return User.register(
        UserId.random(),
        new Email("alice@example.com"),
        new PasswordHash("$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$hashhashhashhashhashhashhash"),
        "Alice",
        Instant.parse("2026-08-26T00:00:00Z"));
  }

  private static PasswordHasher matchingHasher() {
    return new PasswordHasher() {
      public PasswordHash hash(String plaintextPassword) {
        throw new UnsupportedOperationException();
      }

      public boolean matches(String plaintextPassword, PasswordHash passwordHash) {
        return "very-secure-password".equals(plaintextPassword);
      }
    };
  }

  private static PasswordHasher rejectingHasher() {
    return new PasswordHasher() {
      public PasswordHash hash(String plaintextPassword) {
        throw new UnsupportedOperationException();
      }

      public boolean matches(String plaintextPassword, PasswordHash passwordHash) {
        return false;
      }
    };
  }

  private record SingleUserRepository(User user) implements UserRepository {
    public boolean existsByEmail(Email email) {
      return user.email().equals(email);
    }

    public Optional<User> findByEmail(Email email) {
      return user.email().equals(email) ? Optional.of(user) : Optional.empty();
    }

    public Optional<User> findById(UserId id) {
      return user.id().equals(id) ? Optional.of(user) : Optional.empty();
    }

    public User save(User user) {
      return user;
    }
  }
}
