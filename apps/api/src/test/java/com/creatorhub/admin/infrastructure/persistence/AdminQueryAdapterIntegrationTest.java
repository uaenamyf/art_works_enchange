package com.creatorhub.admin.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.admin.application.port.in.AdminUseCases;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 PostgreSQL 验证 Admin 队列状态过滤、Creator 脱敏快照和审核历史游标分页。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class AdminQueryAdapterIntegrationTest {

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

  @Autowired AdminQueryAdapter adminQueries;
  @Autowired JdbcTemplate jdbc;

  private UUID ownerId;
  private UUID reviewerId;
  private UUID creatorId;
  private UUID pendingArtworkId;
  private UUID publishedArtworkId;
  private UUID firstReviewId;
  private UUID secondReviewId;

  @AfterEach
  void cleanUp() {
    if (pendingArtworkId != null) {
      jdbc.update("DELETE FROM artwork_review_records WHERE artwork_id = ?", pendingArtworkId);
      jdbc.update("DELETE FROM artworks WHERE id = ?", pendingArtworkId);
    }
    if (publishedArtworkId != null) {
      jdbc.update("DELETE FROM artwork_review_records WHERE artwork_id = ?", publishedArtworkId);
      jdbc.update("DELETE FROM artworks WHERE id = ?", publishedArtworkId);
    }
    if (creatorId != null) jdbc.update("DELETE FROM creator_profiles WHERE id = ?", creatorId);
    if (ownerId != null) jdbc.update("DELETE FROM users WHERE id = ?", ownerId);
    if (reviewerId != null) jdbc.update("DELETE FROM users WHERE id = ?", reviewerId);
  }

  @Test
  void reviewQueueOnlyReturnsPendingArtworkWithCreatorSnapshot() {
    createUsersAndCreator();
    pendingArtworkId = UUID.randomUUID();
    publishedArtworkId = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-31T00:00:00Z");
    insertArtwork(pendingArtworkId, "Pending artwork", "PENDING_REVIEW", now);
    insertArtwork(publishedArtworkId, "Published artwork", "PUBLISHED", now.plusSeconds(1));

    AdminUseCases.ReviewQueuePage page = adminQueries.findReviewQueue(null, 20);

    assertThat(page.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.id()).isEqualTo(pendingArtworkId);
              assertThat(item.title()).isEqualTo("Pending artwork");
              assertThat(item.creator().id()).isEqualTo(creatorId);
              assertThat(item.creator().displayName()).isEqualTo("Test Creator");
              assertThat(item.status())
                  .isEqualTo(com.creatorhub.catalog.domain.model.ArtworkStatus.PENDING_REVIEW);
            });
    assertThat(page.hasMore()).isFalse();
    assertThat(page.nextCursor()).isNull();
  }

  @Test
  void reviewHistoryUsesStableCursorPagination() {
    createUsersAndCreator();
    pendingArtworkId = UUID.randomUUID();
    Instant firstAt = Instant.parse("2026-08-31T00:00:00Z");
    insertArtwork(pendingArtworkId, "Reviewed artwork", "APPROVED", firstAt);
    firstReviewId = UUID.randomUUID();
    secondReviewId = UUID.randomUUID();
    insertReview(firstReviewId, firstAt, "first reason");
    insertReview(secondReviewId, firstAt.plusSeconds(1), "second reason");

    AdminUseCases.ReviewHistoryPage firstPage = adminQueries.findReviewHistory(null, 1);

    assertThat(firstPage.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.id()).isEqualTo(secondReviewId);
              assertThat(item.artworkId()).isEqualTo(pendingArtworkId);
              assertThat(item.artworkTitle()).isEqualTo("Reviewed artwork");
              assertThat(item.creator().displayName()).isEqualTo("Test Creator");
              assertThat(item.reviewerId()).isEqualTo(reviewerId);
              assertThat(item.reason()).isEqualTo("second reason");
            });
    assertThat(firstPage.hasMore()).isTrue();
    assertThat(firstPage.nextCursor()).isNotBlank();

    AdminUseCases.ReviewHistoryPage secondPage =
        adminQueries.findReviewHistory(firstPage.nextCursor(), 1);

    assertThat(secondPage.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.id()).isEqualTo(firstReviewId);
              assertThat(item.reason()).isEqualTo("first reason");
            });
    assertThat(secondPage.hasMore()).isFalse();
    assertThat(secondPage.nextCursor()).isNull();
  }

  private void createUsersAndCreator() {
    ownerId = UUID.randomUUID();
    reviewerId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.parse("2026-08-31T00:00:00Z"));
    insertUser(ownerId, "owner");
    insertUser(reviewerId, "reviewer");
    jdbc.update(
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        creatorId,
        ownerId,
        "admin-test-" + creatorId.toString().substring(0, 8),
        "Test Creator",
        "ART",
        "Admin query test creator",
        now,
        now);
  }

  private void insertUser(UUID id, String name) {
    Timestamp now = Timestamp.from(Instant.parse("2026-08-31T00:00:00Z"));
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, 'hash', ?, 'CREATOR', ?, ?)",
        id,
        id + "@example.com",
        name,
        now,
        now);
  }

  private void insertArtwork(UUID id, String title, String status, Instant updatedAt) {
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, product_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'ART', ?, 'CNY', 1, ?, 'PHYSICAL', ?, ?)",
        id,
        ownerId,
        creatorId,
        title,
        "Admin query test artwork",
        BigDecimal.TEN,
        status,
        Timestamp.from(updatedAt),
        Timestamp.from(updatedAt));
  }

  private void insertReview(UUID id, Instant createdAt, String reason) {
    jdbc.update(
        "INSERT INTO artwork_review_records (id, artwork_id, reviewer_id, decision, reason, created_at) VALUES (?, ?, ?, 'APPROVED', ?, ?)",
        id,
        pendingArtworkId,
        reviewerId,
        reason,
        Timestamp.from(createdAt));
  }
}
