package com.creatorhub.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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

/** 使用真实 PostgreSQL 验证条件扣减、库存流水和释放幂等的事务边界。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class ArtworkStockAvailabilityIntegrationTest {

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

  @Autowired ArtworkRepositoryAdapter stocks;
  @Autowired JdbcTemplate jdbc;

  private UUID artworkId;

  @AfterEach
  void cleanUp() {
    if (artworkId != null) {
      jdbc.update("DELETE FROM inventory_movements WHERE artwork_id = ?", artworkId);
      jdbc.update("DELETE FROM artworks WHERE id = ?", artworkId);
    }
  }

  @Test
  void concurrentReservationsCannotOversellStock() throws Exception {
    createPublishedArtwork(10);
    ExecutorService executor = Executors.newFixedThreadPool(20);
    try {
      List<Callable<Boolean>> attempts =
          IntStream.range(0, 20)
              .mapToObj(
                  ignored ->
                      (Callable<Boolean>) () -> stocks.reserve(artworkId, 1, UUID.randomUUID()))
              .toList();

      long successfulReservations =
          executor.invokeAll(attempts).stream().filter(future -> get(future)).count();

      assertThat(successfulReservations).isEqualTo(10);
      assertThat(stock()).isZero();
      assertThat(movementCount("RESERVE")).isEqualTo(10);
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void duplicateReleaseReturnsStockOnlyOnce() {
    createPublishedArtwork(1);
    UUID reservationId = UUID.randomUUID();

    assertThat(stocks.reserve(artworkId, 1, reservationId)).isTrue();
    assertThat(stocks.release(artworkId, 1, reservationId)).isTrue();
    assertThat(stocks.release(artworkId, 1, reservationId)).isFalse();

    assertThat(stock()).isEqualTo(1);
    assertThat(movementCount("RELEASE")).isEqualTo(1);
  }

  private void createPublishedArtwork(int stock) {
    UUID userId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    artworkId = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-27T00:00:00Z");
    Timestamp timestamp = Timestamp.from(now);
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'CREATOR', ?, ?)",
        userId,
        userId + "@example.com",
        "hash",
        "Test Creator",
        timestamp,
        timestamp);
    jdbc.update(
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        creatorId,
        userId,
        "test-" + creatorId.toString().substring(0, 8),
        "Test Creator",
        "ILLUSTRATION",
        "Integration test creator",
        timestamp,
        timestamp);
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)",
        artworkId,
        userId,
        creatorId,
        "Integration Artwork",
        "Integration test artwork",
        "ILLUSTRATION",
        BigDecimal.TEN,
        "CNY",
        stock,
        timestamp,
        timestamp);
  }

  private int stock() {
    return jdbc.queryForObject("SELECT stock FROM artworks WHERE id = ?", Integer.class, artworkId);
  }

  private int movementCount(String movementType) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM inventory_movements WHERE artwork_id = ? AND movement_type = ?",
        Integer.class,
        artworkId,
        movementType);
  }

  private static boolean get(java.util.concurrent.Future<Boolean> future) {
    try {
      return future.get();
    } catch (Exception exception) {
      throw new AssertionError("Concurrent inventory attempt failed", exception);
    }
  }
}
