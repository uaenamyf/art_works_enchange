package com.creatorhub.ordering.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.ordering.domain.model.FulfillmentType;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderId;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
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

/** 使用真实 PostgreSQL 验证数字订单保存后仍保持 DIGITAL 和 NULL 收货地址。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class OrderRepositoryFulfillmentIntegrationTest {

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

  @Autowired OrderRepositoryAdapter orders;
  @Autowired JdbcTemplate jdbc;

  private UUID orderId;
  private UUID buyerId;

  @AfterEach
  void cleanUp() {
    if (orderId != null) jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
    if (buyerId != null) jdbc.update("DELETE FROM users WHERE id = ?", buyerId);
  }

  @Test
  void digitalOrderRoundTripsWithoutShippingAddress() {
    buyerId = UUID.randomUUID();
    orderId = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-28T00:00:00Z");
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, 'hash', 'Buyer', 'USER', ?, ?)",
        buyerId,
        buyerId + "@example.com",
        Timestamp.from(now),
        Timestamp.from(now));
    Order order =
        Order.create(
            OrderId.of(orderId),
            UserId.of(buyerId),
            "CH" + orderId.toString().replace("-", "").substring(0, 18),
            new OrderLine(ArtworkId.random(), "Preset", Money.of("10", "CNY"), 1),
            (ShippingAddress) null,
            now,
            now.plusSeconds(1800),
            FulfillmentType.DIGITAL);

    orders.save(order);

    Order restored = orders.findById(orderId).orElseThrow();
    assertThat(restored.fulfillmentType()).isEqualTo(FulfillmentType.DIGITAL);
    assertThat(restored.shippingAddress()).isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT fulfillment_type FROM orders WHERE id = ?", String.class, orderId))
        .isEqualTo("DIGITAL");
  }
}
