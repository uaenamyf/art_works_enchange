package com.creatorhub.digitaldelivery.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.digitaldelivery.domain.model.DownloadGrant;
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

/** 使用真实 PostgreSQL 验证下载凭证只保存摘要，并且有效期内只能核销一次。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class DownloadGrantRepositoryIntegrationTest {

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

  @Autowired DownloadGrantRepositoryAdapter grants;
  @Autowired JdbcTemplate jdbc;

  private UUID entitlementId;

  @AfterEach
  void cleanUp() {
    if (entitlementId != null)
      jdbc.update("DELETE FROM digital_download_grants WHERE entitlement_id = ?", entitlementId);
  }

  @Test
  void storesDigestAndMarksGrantUsedOnlyOnce() {
    entitlementId = UUID.randomUUID();
    UUID grantId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    createEntitlement();
    DownloadGrant grant =
        DownloadGrant.issue(
            grantId,
            entitlementId,
            "a".repeat(64),
            createdAt.plusSeconds(300),
            null,
            null,
            createdAt);

    grants.save(grant);

    assertThat(
            jdbc.queryForObject(
                "SELECT token_digest FROM digital_download_grants WHERE id = ?",
                String.class,
                grantId))
        .isEqualTo("a".repeat(64));
    assertThat(grants.markUsed(grantId, Instant.now())).isTrue();
    assertThat(grants.markUsed(grantId, Instant.now())).isFalse();
  }

  private void createEntitlement() {
    UUID buyerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    UUID assetId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, 'hash', 'Buyer', 'USER', ?, ?)",
        buyerId,
        buyerId + "@example.com",
        now,
        now);
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, 'hash', 'Owner', 'CREATOR', ?, ?)",
        ownerId,
        ownerId + "@example.com",
        now,
        now);
    jdbc.update(
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, 'Owner', 'DIGITAL', 'Grant test', ?, ?)",
        creatorId,
        ownerId,
        "grant-" + creatorId.toString().substring(0, 8),
        now,
        now);
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, product_type, created_at, updated_at) VALUES (?, ?, ?, 'Digital', 'Grant test artwork', 'DIGITAL', 10.00, 'CNY', 0, 'PUBLISHED', 'DIGITAL', ?, ?)",
        artworkId,
        ownerId,
        creatorId,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_assets (id, artwork_id, owner_id, status, current_version, created_at, updated_at) VALUES (?, ?, ?, 'READY', 1, ?, ?)",
        assetId,
        artworkId,
        ownerId,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_asset_versions (id, digital_asset_id, version, file_name, object_key, content_type, size_bytes, sha256, scan_status, created_at) VALUES (?, ?, 1, 'asset.zip', 'private/grant-asset', 'application/zip', 1024, ?, 'READY', ?)",
        versionId,
        assetId,
        "b".repeat(64),
        now);
    jdbc.update(
        "INSERT INTO orders (id, order_number, buyer_id, artwork_id, title, unit_price, currency, quantity, subtotal, status, shipping_address, expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, 'Digital', 10.00, 'CNY', 1, 10.00, 'PAID', '{}'::jsonb, ?, ?, ?)",
        orderId,
        "CH" + orderId.toString().replace("-", "").substring(0, 18),
        buyerId,
        artworkId,
        now,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_entitlements (id, order_id, buyer_id, artwork_id, asset_version_id, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'AVAILABLE', ?, ?)",
        entitlementId,
        orderId,
        buyerId,
        artworkId,
        versionId,
        now,
        now);
  }
}
