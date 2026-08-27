package com.creatorhub.ordering.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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

/** 使用真实 PostgreSQL 验证订单幂等键在并发创建请求下只有一个 claim 获胜。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class OrderIdempotencyIntegrationTest {

  private static final String OPERATION = "createOrder";
  private static final String IDEMPOTENCY_KEY = "same-order-key";
  private static final String REQUEST_HASH = "a".repeat(64);

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

  @Autowired OrderIdempotencyMapper idempotency;
  @Autowired JdbcTemplate jdbc;

  private UUID buyerId;

  @AfterEach
  void cleanUp() {
    if (buyerId != null) {
      jdbc.update("DELETE FROM order_idempotency_keys WHERE buyer_id = ?", buyerId);
      jdbc.update("DELETE FROM users WHERE id = ?", buyerId);
    }
  }

  @Test
  void concurrentClaimsHaveOneWinner() throws Exception {
    buyerId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'USER', ?, ?)",
        buyerId,
        buyerId + "@example.com",
        "hash",
        "Test Buyer",
        Timestamp.from(Instant.now()),
        Timestamp.from(Instant.now()));

    ExecutorService executor = Executors.newFixedThreadPool(20);
    try {
      List<Callable<Integer>> attempts =
          IntStream.range(0, 20)
              .mapToObj(
                  ignored ->
                      (Callable<Integer>)
                          () ->
                              idempotency.claim(
                                  buyerId, OPERATION, IDEMPOTENCY_KEY, REQUEST_HASH, Instant.now()))
              .toList();

      long winners =
          executor.invokeAll(attempts).stream().filter(future -> get(future) == 1).count();

      assertThat(winners).isEqualTo(1);
      assertThat(rowCount()).isEqualTo(1);
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private int rowCount() {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM order_idempotency_keys WHERE buyer_id = ? AND operation = ? AND idempotency_key = ?",
        Integer.class,
        buyerId,
        OPERATION,
        IDEMPOTENCY_KEY);
  }

  private static int get(java.util.concurrent.Future<Integer> future) {
    try {
      return future.get();
    } catch (Exception exception) {
      throw new AssertionError("Concurrent idempotency claim failed", exception);
    }
  }
}
