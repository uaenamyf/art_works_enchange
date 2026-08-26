package com.creatorhub.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.PasswordHash;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.infrastructure.security.JwtSessionIssuer;
import com.creatorhub.identity.infrastructure.security.PersistentSessionManager;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
/** 使用 PostgreSQL 17 验证 Flyway V1、User 聚合/MyBatis 映射和会话单次消费。 */
class UserRepositoryAdapterTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("creatorhub_test")
          .withUsername("creatorhub")
          .withPassword("creatorhub");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired UserRepositoryAdapter users;
  @Autowired SessionStore sessionStore;

  @Test
  void flywayCreatesUsersTableAndAdapterRoundTripsAggregate() {
    User user =
        User.register(
            UserId.random(),
            new Email("alice@example.com"),
            new PasswordHash("$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$hashhashhashhashhashhashhash"),
            "Alice",
            Instant.parse("2026-08-26T00:00:00Z"));

    users.save(user);

    assertThat(users.existsByEmail(new Email("ALICE@example.com"))).isTrue();
    assertThat(users.findByEmail(new Email("alice@example.com")))
        .get()
        .satisfies(
            saved -> {
              assertThat(saved.id()).isEqualTo(user.id());
              assertThat(saved.displayName()).isEqualTo("Alice");
              assertThat(saved.createdAt()).isEqualTo(user.createdAt());
            });
  }

  @Test
  void refreshTokenCanBeRotatedOnlyOnce() {
    User user =
        User.register(
            UserId.random(),
            new Email("rotate@example.com"),
            new PasswordHash("$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$hashhashhashhashhashhashhash"),
            "Rotate User",
            Instant.parse("2026-08-26T00:00:00Z"));
    users.save(user);

    JwtSessionIssuer issuer =
        new JwtSessionIssuer(
            sessionStore, "change-me-please-change-me-please-32bytes", "creatorhub", 900, 1209600);
    PersistentSessionManager manager = new PersistentSessionManager(sessionStore, users, issuer);
    String firstRefresh = issuer.issue(user).refreshToken();

    var rotated = manager.rotate(firstRefresh);

    assertThat(rotated.session().refreshToken()).isNotEqualTo(firstRefresh);
    assertThatThrownBy(() -> manager.rotate(firstRefresh))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("REFRESH_TOKEN_INVALID");
  }
}
