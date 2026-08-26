package com.creatorhub.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.sharedkernel.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 验证 User 聚合的注册默认值、禁用规则以及邮箱/展示名不变量。 */
class UserTest {

  private static final PasswordHash PASSWORD_HASH =
      new PasswordHash("$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$hashhashhashhashhashhashhash");

  @Test
  void registerNormalizesEmailAndStartsAsActiveUser() {
    User user =
        User.register(
            UserId.random(),
            new Email("  Alice@Example.COM "),
            PASSWORD_HASH,
            "Alice",
            Instant.parse("2026-08-26T00:00:00Z"));

    assertThat(user.email().value()).isEqualTo("alice@example.com");
    assertThat(user.role()).isEqualTo(UserRole.USER);
    assertThat(user.canIssueSession()).isTrue();
  }

  @Test
  void disabledUserCannotBecomeCreatorOrIssueSession() {
    User user =
        User.register(
            UserId.random(),
            new Email("alice@example.com"),
            PASSWORD_HASH,
            "Alice",
            Instant.parse("2026-08-26T00:00:00Z"));

    user.disable();

    assertThat(user.canIssueSession()).isFalse();
    assertThatThrownBy(user::promoteToCreator)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Disabled user");
  }

  @Test
  void rejectsInvalidEmailAndDisplayName() {
    assertThatThrownBy(() -> new Email("not-an-email"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                User.register(
                    UserId.random(),
                    new Email("alice@example.com"),
                    PASSWORD_HASH,
                    "A",
                    Instant.parse("2026-08-26T00:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Display name");
  }
}
