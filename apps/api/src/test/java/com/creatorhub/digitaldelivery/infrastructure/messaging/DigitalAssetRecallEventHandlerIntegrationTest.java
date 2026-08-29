package com.creatorhub.digitaldelivery.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
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

/** 使用真实 PostgreSQL 验证撤回事件会使授权和未使用下载凭证失效。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class DigitalAssetRecallEventHandlerIntegrationTest {

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

  @Autowired DigitalAssetRecallEventHandler handler;
  @Autowired JdbcTemplate jdbc;

  private UUID buyerId;
  private UUID assetVersionId;
  private UUID entitlementId;
  private UUID grantId;
  private UUID artworkId;
  private UUID assetId;
  private UUID orderId;

  @AfterEach
  void cleanUp() {
    jdbc.update("DELETE FROM digital_download_grants WHERE id = ?", grantId);
    jdbc.update("DELETE FROM digital_entitlements WHERE id = ?", entitlementId);
    jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
    jdbc.update("DELETE FROM digital_asset_versions WHERE id = ?", assetVersionId);
    jdbc.update("DELETE FROM digital_assets WHERE id = ?", assetId);
    jdbc.update("DELETE FROM artworks WHERE id = ?", artworkId);
    jdbc.update("DELETE FROM users WHERE id = ?", buyerId);
  }

  @Test
  void recallRevokesAvailableEntitlementAndUnusedGrantIdempotently() {
    createFixture();
    Instant recalledAt = Instant.parse("2026-08-29T01:00:00Z");
    OutboxEvent event =
        OutboxEvent.pending(
            UUID.randomUUID(),
            "DigitalAssetRecalled",
            1,
            "DigitalAsset",
            assetId,
            1,
            recalledAt,
            UUID.randomUUID(),
            Map.of("assetVersionId", assetVersionId.toString(), "reasonCode", "content update"));

    handler.handle(event);
    handler.handle(event);

    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM digital_entitlements WHERE id = ?",
                String.class,
                entitlementId))
        .isEqualTo("REVOKED");
    assertThat(
            jdbc.queryForObject(
                "SELECT revoked_at FROM digital_download_grants WHERE id = ?",
                Timestamp.class,
                grantId))
        .isEqualTo(Timestamp.from(recalledAt));
  }

  private void createFixture() {
    buyerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    artworkId = UUID.randomUUID();
    assetId = UUID.randomUUID();
    assetVersionId = UUID.randomUUID();
    orderId = UUID.randomUUID();
    entitlementId = UUID.randomUUID();
    grantId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.parse("2026-08-29T00:00:00Z"));
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, 'hash', 'Buyer', 'USER', ?, ?), (?, ?, 'hash', 'Owner', 'CREATOR', ?, ?)",
        buyerId,
        buyerId + "@example.com",
        now,
        now,
        ownerId,
        ownerId + "@example.com",
        now,
        now);
    jdbc.update(
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, 'Owner', 'DIGITAL', 'test', ?, ?)",
        creatorId,
        ownerId,
        "recall-" + creatorId.toString().substring(0, 8),
        now,
        now);
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, product_type, created_at, updated_at) VALUES (?, ?, ?, 'Digital', 'test', 'DIGITAL', ?, 'CNY', 0, 'PUBLISHED', 'DIGITAL', ?, ?)",
        artworkId,
        ownerId,
        creatorId,
        BigDecimal.TEN,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_assets (id, artwork_id, owner_id, status, current_version, created_at, updated_at) VALUES (?, ?, ?, 'RECALLED', 1, ?, ?)",
        assetId,
        artworkId,
        ownerId,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_asset_versions (id, digital_asset_id, version, file_name, object_key, content_type, size_bytes, sha256, scan_status, created_at) VALUES (?, ?, 1, 'asset.zip', 'private/recall-asset', 'application/zip', 1024, ?, 'READY', ?)",
        assetVersionId,
        assetId,
        "c".repeat(64),
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
        "INSERT INTO digital_entitlements (id, order_id, buyer_id, artwork_id, asset_version_id, status, granted_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'AVAILABLE', ?, ?, ?)",
        entitlementId,
        orderId,
        buyerId,
        artworkId,
        assetVersionId,
        now,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_download_grants (id, entitlement_id, token_digest, expires_at, created_at) VALUES (?, ?, ?, ?, ?)",
        grantId,
        entitlementId,
        "d".repeat(64),
        Timestamp.from(now.toInstant().plusSeconds(300)),
        now);
  }
}
