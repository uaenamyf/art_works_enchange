package com.creatorhub.digitaldelivery.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import com.creatorhub.digitaldelivery.domain.model.EntitlementStatus;
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

/** 使用真实 PostgreSQL 验证数字授权的 READY 资产门禁和订单唯一幂等。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class EntitlementRepositoryIntegrationTest {

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

  @Autowired EntitlementRepositoryAdapter entitlements;
  @Autowired JdbcTemplate jdbc;

  private UUID buyerId;
  private UUID orderId;
  private UUID artworkId;
  private UUID assetId;
  private UUID versionId;

  @AfterEach
  void cleanUp() {
    if (orderId != null)
      jdbc.update("DELETE FROM digital_entitlements WHERE order_id = ?", orderId);
    if (orderId != null) jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
    if (versionId != null)
      jdbc.update("DELETE FROM digital_asset_versions WHERE id = ?", versionId);
    if (assetId != null) jdbc.update("DELETE FROM digital_assets WHERE id = ?", assetId);
    if (artworkId != null) jdbc.update("DELETE FROM artworks WHERE id = ?", artworkId);
    if (buyerId != null) jdbc.update("DELETE FROM users WHERE id = ?", buyerId);
  }

  @Test
  void pendingEntitlementRequiresReadyAsset() {
    createFixture("PENDING_SCAN");
    Entitlement entitlement = pendingEntitlement();

    assertThatThrownBy(() -> entitlements.save(entitlement))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("DIGITAL_ASSET_NOT_READY");
  }

  @Test
  void sameOrderIsIdempotentAfterAssetIsReady() {
    createFixture("READY");
    Entitlement first = pendingEntitlement();

    Entitlement saved = entitlements.save(first);
    Entitlement replay = entitlements.save(pendingEntitlement());

    assertThat(saved.id()).isEqualTo(first.id());
    assertThat(replay.id()).isEqualTo(first.id());
    assertThat(replay.status()).isEqualTo(EntitlementStatus.PENDING);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM digital_entitlements WHERE order_id = ?",
                Integer.class,
                orderId))
        .isOne();
  }

  private Entitlement pendingEntitlement() {
    return Entitlement.pending(
        UUID.randomUUID(), orderId, buyerId, artworkId, versionId, Instant.now());
  }

  private void createFixture(String scanStatus) {
    buyerId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    artworkId = UUID.randomUUID();
    assetId = UUID.randomUUID();
    versionId = UUID.randomUUID();
    orderId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'USER', ?, ?)",
        buyerId,
        buyerId + "@example.com",
        "hash",
        "Test Buyer",
        now,
        now);
    UUID ownerId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'CREATOR', ?, ?)",
        ownerId,
        ownerId + "@example.com",
        "hash",
        "Test Creator",
        now,
        now);
    creatorId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO creator_profiles (id, user_id, slug, display_name, category, bio, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        creatorId,
        ownerId,
        "ent-" + creatorId.toString().substring(0, 8),
        "Test Creator",
        "DIGITAL",
        "Entitlement test creator",
        now,
        now);
    jdbc.update(
        "INSERT INTO artworks (id, owner_id, creator_id, title, description, category, price_amount, price_currency, stock, status, product_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 'PUBLISHED', 'DIGITAL', ?, ?)",
        artworkId,
        ownerId,
        creatorId,
        "Digital Artwork",
        "Digital entitlement integration test",
        "DIGITAL",
        BigDecimal.TEN,
        "CNY",
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
        "INSERT INTO digital_asset_versions (id, digital_asset_id, version, file_name, object_key, content_type, size_bytes, sha256, scan_status, created_at) VALUES (?, ?, 1, 'asset.zip', 'private/asset.zip', 'application/zip', 1024, ?, ?, ?)",
        versionId,
        assetId,
        "a".repeat(64),
        scanStatus,
        now);
    jdbc.update(
        "INSERT INTO orders (id, order_number, buyer_id, artwork_id, title, unit_price, currency, quantity, subtotal, status, shipping_address, expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 10.00, 'CNY', 1, 10.00, 'PAID', '{}'::jsonb, ?, ?, ?)",
        orderId,
        "CH" + orderId.toString().replace("-", "").substring(0, 18),
        buyerId,
        artworkId,
        "Digital Artwork",
        now,
        now,
        now);
  }
}
