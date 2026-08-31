package com.creatorhub.studio.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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

/** 使用真实 PostgreSQL 验证 Studio Dashboard 的订单、作品和关注者聚合。 */
@SpringBootTest(properties = "spring.flyway.enabled=true")
@Testcontainers
class StudioDashboardQueryAdapterIntegrationTest {

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

  @Autowired StudioDashboardQueryAdapter dashboard;
  @Autowired JdbcTemplate jdbc;

  private UUID ownerId;
  private UUID creatorId;
  private UUID artworkId;
  private UUID digitalArtworkId;
  private UUID buyerId;
  private UUID digitalOrderId;
  private UUID entitlementId;
  private UUID assetId;
  private UUID versionId;
  private UUID grantId;

  @AfterEach
  void cleanUp() {
    if (artworkId != null) jdbc.update("DELETE FROM orders WHERE artwork_id = ?", artworkId);
    if (grantId != null) jdbc.update("DELETE FROM digital_download_grants WHERE id = ?", grantId);
    if (entitlementId != null)
      jdbc.update("DELETE FROM digital_entitlements WHERE id = ?", entitlementId);
    if (digitalOrderId != null) jdbc.update("DELETE FROM orders WHERE id = ?", digitalOrderId);
    if (versionId != null)
      jdbc.update("DELETE FROM digital_asset_versions WHERE id = ?", versionId);
    if (assetId != null) jdbc.update("DELETE FROM digital_assets WHERE id = ?", assetId);
    if (digitalArtworkId != null)
      jdbc.update("DELETE FROM artworks WHERE id = ?", digitalArtworkId);
    if (ownerId != null) jdbc.update("DELETE FROM creator_follows WHERE creator_id = ?", creatorId);
    if (artworkId != null) jdbc.update("DELETE FROM artworks WHERE id = ?", artworkId);
    if (creatorId != null) jdbc.update("DELETE FROM creator_profiles WHERE id = ?", creatorId);
    if (buyerId != null) jdbc.update("DELETE FROM users WHERE id = ?", buyerId);
    if (ownerId != null) jdbc.update("DELETE FROM users WHERE id = ?", ownerId);
  }

  @Test
  void aggregatesOnlyPublishedArtworksAndPaidOrders() {
    createFixture();

    var view = dashboard.findByUserId(ownerId);

    assertThat(view.revenue().amount()).isEqualByComparingTo("30.00");
    assertThat(view.revenue().currency().getCurrencyCode()).isEqualTo("CNY");
    assertThat(view.orderCount()).isEqualTo(2);
    assertThat(view.publishedArtworkCount()).isEqualTo(2);
    assertThat(view.followerCount()).isEqualTo(1);
    assertThat(view.digitalDownloadCount()).isEqualTo(1);
  }

  private void createFixture() {
    ownerId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    artworkId = UUID.randomUUID();
    buyerId = UUID.randomUUID();
    Instant instant = Instant.parse("2026-08-29T00:00:00Z");
    Timestamp now = Timestamp.from(instant);
    insertUser(ownerId, "owner");
    insertUser(buyerId, "buyer");
    jdbc.update(
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        creatorId,
        ownerId,
        "studio-" + ownerId.toString().substring(0, 8),
        "Studio Owner",
        "ART",
        "Bio",
        now,
        now);
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)",
        artworkId,
        ownerId,
        creatorId,
        "Studio Work",
        "A sufficiently long description",
        "ART",
        BigDecimal.TEN,
        "CNY",
        3,
        now,
        now);
    jdbc.update(
        "INSERT INTO creator_follows (user_id, creator_id, created_at) VALUES (?, ?, ?)",
        buyerId,
        creatorId,
        now);
    jdbc.update(
        "INSERT INTO orders (id, order_number, buyer_id, artwork_id, title, unit_price, currency, quantity, subtotal, status, fulfillment_type, shipping_address, expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PHYSICAL', CAST(? AS jsonb), ?, ?, ?)",
        UUID.randomUUID(),
        "CH" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
        buyerId,
        artworkId,
        "Studio Work",
        BigDecimal.TEN,
        "CNY",
        2,
        BigDecimal.valueOf(20),
        "PAID",
        "{}",
        Timestamp.from(instant.plusSeconds(1800)),
        now,
        now);
    jdbc.update(
        "INSERT INTO orders (id, order_number, buyer_id, artwork_id, title, unit_price, currency, quantity, subtotal, status, fulfillment_type, shipping_address, expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PHYSICAL', CAST(? AS jsonb), ?, ?, ?)",
        UUID.randomUUID(),
        "CH" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
        buyerId,
        artworkId,
        "Studio Work",
        BigDecimal.TEN,
        "CNY",
        1,
        BigDecimal.TEN,
        "PENDING_PAYMENT",
        "{}",
        Timestamp.from(instant.plusSeconds(1800)),
        now,
        now);
    createDigitalDownloadFixture(instant, now);
  }

  private void createDigitalDownloadFixture(Instant instant, Timestamp now) {
    digitalArtworkId = UUID.randomUUID();
    digitalOrderId = UUID.randomUUID();
    assetId = UUID.randomUUID();
    versionId = UUID.randomUUID();
    entitlementId = UUID.randomUUID();
    grantId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, product_type, created_at, updated_at) VALUES (?, ?, ?, 'Digital Work', 'Digital dashboard artwork', 'DIGITAL', 10.00, 'CNY', 0, 'PUBLISHED', 'DIGITAL', ?, ?)",
        digitalArtworkId,
        ownerId,
        creatorId,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_assets (id, artwork_id, owner_id, status, current_version, created_at, updated_at) VALUES (?, ?, ?, 'READY', 1, ?, ?)",
        assetId,
        digitalArtworkId,
        ownerId,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_asset_versions (id, digital_asset_id, version, file_name, object_key, content_type, size_bytes, sha256, scan_status, created_at) VALUES (?, ?, 1, 'asset.zip', ?, 'application/zip', 1024, ?, 'READY', ?)",
        versionId,
        assetId,
        "private/dashboard-" + versionId,
        "a".repeat(64),
        now);
    jdbc.update(
        "INSERT INTO orders (id, order_number, buyer_id, artwork_id, title, unit_price, currency, quantity, subtotal, status, fulfillment_type, shipping_address, expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, 'Digital Work', 10.00, 'CNY', 1, 10.00, 'PAID', 'DIGITAL', NULL, ?, ?, ?)",
        digitalOrderId,
        "CH" + digitalOrderId.toString().replace("-", "").substring(0, 18),
        buyerId,
        digitalArtworkId,
        Timestamp.from(instant.plusSeconds(1800)),
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_entitlements (id, order_id, buyer_id, artwork_id, asset_version_id, status, granted_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'AVAILABLE', ?, ?, ?)",
        entitlementId,
        digitalOrderId,
        buyerId,
        digitalArtworkId,
        versionId,
        now,
        now,
        now);
    jdbc.update(
        "INSERT INTO digital_download_grants (id, entitlement_id, token_digest, expires_at, used_at, request_ip_hash, user_agent_hash, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        grantId,
        entitlementId,
        "b".repeat(64),
        Timestamp.from(instant.plusSeconds(300)),
        Timestamp.from(instant.plusSeconds(10)),
        "c".repeat(64),
        "d".repeat(64),
        now);
  }

  private void insertUser(UUID id, String name) {
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'USER', ?, ?)",
        id,
        name + "-" + id + "@example.com",
        "hash",
        name,
        Timestamp.from(Instant.parse("2026-08-29T00:00:00Z")),
        Timestamp.from(Instant.parse("2026-08-29T00:00:00Z")));
  }
}
