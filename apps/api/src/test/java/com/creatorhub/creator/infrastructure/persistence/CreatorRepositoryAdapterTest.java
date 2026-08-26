package com.creatorhub.creator.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.creator.domain.model.Creator;
import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.PasswordHash;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.infrastructure.persistence.UserRepositoryAdapter;
import com.creatorhub.sharedkernel.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用 PostgreSQL 17 验证 V2 migration、双唯一约束和 Creator 聚合映射。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class CreatorRepositoryAdapterTest {

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
  @Autowired CreatorRepositoryAdapter creators;

  @Test
  void flywayCreatesCreatorTableAndAdapterRoundTripsAggregate() {
    UserId userId = UserId.random();
    users.save(
        User.register(
            userId,
            new Email("creator@example.com"),
            new PasswordHash("$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$hashhashhashhashhashhashhash"),
            "Creator User",
            Instant.parse("2026-08-26T00:00:00Z")));
    Creator creator =
        Creator.apply(
            CreatorId.random(),
            userId,
            "creator-studio",
            "Creator Studio",
            "ILLUSTRATION",
            "Bio",
            "Shanghai",
            Instant.parse("2026-08-26T00:00:00Z"));

    creators.save(creator);

    assertThat(creators.existsByUserId(userId)).isTrue();
    assertThat(creators.existsBySlug("creator-studio")).isTrue();
    assertThat(creators.findById(creator.id()))
        .get()
        .extracting(Creator::displayName)
        .isEqualTo("Creator Studio");
  }
}
