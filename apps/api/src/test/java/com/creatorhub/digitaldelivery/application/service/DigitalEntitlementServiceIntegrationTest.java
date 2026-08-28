package com.creatorhub.digitaldelivery.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import com.creatorhub.digitaldelivery.infrastructure.persistence.EntitlementRepositoryAdapter;
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

/** 使用真实 PostgreSQL 验证支付确认后授权可用，并由订单唯一键处理重复事件。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class DigitalEntitlementServiceIntegrationTest {

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

  @Autowired DigitalEntitlementService service;
  @Autowired EntitlementRepositoryAdapter entitlements;
  @Autowired JdbcTemplate jdbc;

  private UUID buyerId;
  private UUID orderId;
  private UUID artworkId;
  private UUID assetVersionId;

  @AfterEach
  void cleanUp() {
    if (orderId != null)
      jdbc.update("DELETE FROM digital_entitlements WHERE order_id = ?", orderId);
    if (orderId != null) jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
    if (assetVersionId != null)
      jdbc.update("DELETE FROM digital_asset_versions WHERE id = ?", assetVersionId);
    if (artworkId != null)
      jdbc.update("DELETE FROM digital_assets WHERE artwork_id = ?", artworkId);
    if (artworkId != null) jdbc.update("DELETE FROM artworks WHERE id = ?", artworkId);
    if (buyerId != null) jdbc.update("DELETE FROM users WHERE id = ?", buyerId);
  }

  @Test
  void trustedPaymentCreatesAvailableEntitlementAndReplayKeepsSameId() {
    createFixture();
    UUID paymentEventId = UUID.randomUUID();
    Instant paidAt = Instant.parse("2026-08-28T01:00:00Z");

    Entitlement first =
        service.grantAfterPayment(
            paymentEventId, orderId, buyerId, artworkId, assetVersionId, paidAt);
    Entitlement replay =
        service.grantAfterPayment(
            UUID.randomUUID(), orderId, buyerId, artworkId, assetVersionId, paidAt.plusSeconds(1));

    assertThat(first.status())
        .isEqualTo(com.creatorhub.digitaldelivery.domain.model.EntitlementStatus.AVAILABLE);
    assertThat(replay.id()).isEqualTo(first.id());
    assertThat(entitlements.findAvailableByBuyer(buyerId, orderId))
        .get()
        .extracting(Entitlement::id)
        .isEqualTo(first.id());
  }

  private void createFixture() {
    buyerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    artworkId = UUID.randomUUID();
    UUID assetId = UUID.randomUUID();
    assetVersionId = UUID.randomUUID();
    orderId = UUID.randomUUID();
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
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, 'Owner', 'DIGITAL', 'test', ?, ?)",
        creatorId,
        ownerId,
        "pay-" + creatorId.toString().substring(0, 8),
        now,
        now);
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, product_type, created_at, updated_at) VALUES (?, ?, ?, 'Digital', 'Payment test artwork', 'DIGITAL', 10.00, 'CNY', 0, 'PUBLISHED', 'DIGITAL', ?, ?)",
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
        "INSERT INTO digital_asset_versions (id, digital_asset_id, version, file_name, object_key, content_type, size_bytes, sha256, scan_status, created_at) VALUES (?, ?, 1, 'asset.zip', 'private/payment-asset', 'application/zip', 1024, ?, 'READY', ?)",
        assetVersionId,
        assetId,
        "a".repeat(64),
        now);
    jdbc.update(
        "INSERT INTO orders (id, order_number, buyer_id, artwork_id, title, unit_price, currency, quantity, subtotal, status, fulfillment_type, shipping_address, expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, 'Digital', 10.00, 'CNY', 1, 10.00, 'PAID', 'DIGITAL', NULL, ?, ?, ?)",
        orderId,
        "CH" + orderId.toString().replace("-", "").substring(0, 18),
        buyerId,
        artworkId,
        now,
        now,
        now);
  }
}
