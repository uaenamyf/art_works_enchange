package com.creatorhub.outbox.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.model.OutboxStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

/** 使用真实 PostgreSQL 验证 Outbox 的 SKIP LOCKED 并发领取不会重复分配事件。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class OutboxRepositoryIntegrationTest {

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

  @Autowired OutboxRepositoryAdapter outbox;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void cleanUp() {
    jdbc.update("DELETE FROM outbox_events");
  }

  @Test
  void concurrentWorkersClaimEachEventOnlyOnce() throws Exception {
    Instant occurredAt = Instant.parse("2026-08-27T00:00:00Z");
    List<UUID> eventIds =
        java.util.stream.IntStream.range(0, 4)
            .mapToObj(
                index -> {
                  UUID eventId = UUID.randomUUID();
                  outbox.append(
                      OutboxEvent.pending(
                          eventId,
                          "OrderCreated",
                          1,
                          "Order",
                          UUID.randomUUID(),
                          1,
                          occurredAt.plusSeconds(index),
                          null,
                          Map.of("index", index)));
                  return eventId;
                })
            .toList();

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<List<OutboxEvent>> claim =
          () -> outbox.claimBatch(Instant.now(), 2, Duration.ofMinutes(5));
      List<Future<List<OutboxEvent>>> futures =
          List.of(executor.submit(claim), executor.submit(claim));
      List<UUID> first = futures.get(0).get().stream().map(OutboxEvent::eventId).toList();
      List<UUID> second = futures.get(1).get().stream().map(OutboxEvent::eventId).toList();
      HashSet<UUID> claimed = new HashSet<>(first);
      claimed.addAll(second);

      assertThat(first).hasSize(2);
      assertThat(second).hasSize(2);
      assertThat(claimed).containsExactlyInAnyOrderElementsOf(eventIds);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM outbox_events WHERE status = ?",
                  Integer.class,
                  OutboxStatus.PROCESSING.name()))
          .isEqualTo(4);
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void deliveredAndRetryTransitionsAreConditional() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-08-27T00:00:00Z");
    outbox.append(
        OutboxEvent.pending(
            eventId, "OrderCreated", 1, "Order", UUID.randomUUID(), 1, occurredAt, null, Map.of()));

    assertThat(outbox.claimBatch(Instant.now(), 1, Duration.ofMinutes(5))).hasSize(1);
    assertThat(outbox.markDelivered(eventId, Instant.now())).isTrue();
    assertThat(outbox.scheduleRetry(eventId, Instant.now(), "late retry")).isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM outbox_events WHERE event_id = ?", String.class, eventId))
        .isEqualTo("DELIVERED");
  }
}
