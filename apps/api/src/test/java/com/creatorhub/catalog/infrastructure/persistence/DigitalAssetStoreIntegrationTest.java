package com.creatorhub.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.catalog.domain.model.DigitalAssetScanStatus;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import com.creatorhub.sharedkernel.error.BusinessError;
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

/** 使用真实 PostgreSQL 验证数字资产归属、版本递增和扫描状态机。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class DigitalAssetStoreIntegrationTest {

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

  @Autowired DigitalAssetStoreAdapter assets;
  @Autowired JdbcTemplate jdbc;

  private UUID artworkId;
  private UUID userId;
  private UUID creatorId;

  @AfterEach
  void cleanUp() {
    if (artworkId != null) {
      jdbc.update("DELETE FROM artworks WHERE id = ?", artworkId);
      jdbc.update("DELETE FROM creator_profiles WHERE id = ?", creatorId);
      jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }
  }

  @Test
  void digitalVersionAdvancesThroughScanStates() {
    createArtwork("DIGITAL");
    DigitalAssetVersion version =
        assets.createPendingVersion(
            userId, artworkId, "preset.zip", "application/zip", 1024, "A".repeat(64));

    assertThat(version.version()).isOne();
    assertThat(version.scanStatus()).isEqualTo(DigitalAssetScanStatus.PENDING_SCAN);
    assertThat(assets.markScanStarted(version.id()).scanStatus())
        .isEqualTo(DigitalAssetScanStatus.SCANNING);
    assertThat(assets.markScanPassed(version.id(), Instant.now()).scanStatus())
        .isEqualTo(DigitalAssetScanStatus.READY);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM digital_assets WHERE id = ?",
                String.class,
                version.digitalAssetId()))
        .isEqualTo("READY");
  }

  @Test
  void physicalArtworkCannotOwnDigitalAsset() {
    createArtwork("PHYSICAL");

    assertThatThrownBy(
            () ->
                assets.createPendingVersion(
                    userId, artworkId, "preset.zip", "application/zip", 1024, "A".repeat(64)))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("DIGITAL_ARTWORK_NOT_FOUND");
  }

  private void createArtwork(String productType) {
    userId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    artworkId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'CREATOR', ?, ?)",
        userId,
        userId + "@example.com",
        "hash",
        "Test Creator",
        now,
        now);
    jdbc.update(
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        creatorId,
        userId,
        "asset-" + creatorId.toString().substring(0, 8),
        "Test Creator",
        "DIGITAL",
        "Asset test creator",
        now,
        now);
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, product_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?, ?)",
        artworkId,
        userId,
        creatorId,
        "Digital Artwork",
        "Digital asset integration test",
        "DIGITAL",
        BigDecimal.TEN,
        "CNY",
        0,
        productType,
        now,
        now);
  }
}
