package com.creatorhub.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.notification.domain.model.Notification;
import com.creatorhub.notification.domain.model.NotificationType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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

/** 使用真实 PostgreSQL 验证通知事件幂等、游标排序和已读用户隔离。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.flyway.enabled=true")
@Testcontainers
class NotificationRepositoryIntegrationTest {

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

  @Autowired NotificationRepositoryAdapter notifications;
  @Autowired JdbcTemplate jdbc;
  private UUID userId;
  private UUID otherUserId;

  @AfterEach
  void cleanUp() {
    jdbc.update("DELETE FROM notifications WHERE user_id IN (?, ?)", userId, otherUserId);
    jdbc.update("DELETE FROM notification_events");
    jdbc.update("DELETE FROM users WHERE id IN (?, ?)", userId, otherUserId);
  }

  @Test
  void duplicateEventReceiptDoesNotCreateDuplicateNotification() {
    createUsers();
    UUID eventId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-08-28T00:00:00Z");
    assertThat(notifications.recordEvent(eventId, "OrderCreated", 1, createdAt)).isTrue();
    assertThat(notifications.recordEvent(eventId, "OrderCreated", 1, createdAt)).isFalse();
    assertThat(notifications.save(notification(eventId, createdAt))).isTrue();
    assertThat(notifications.save(notification(eventId, createdAt.plusSeconds(1)))).isFalse();
  }

  @Test
  void listsByCursorAndCannotMarkOtherUsersNotificationRead() {
    createUsers();
    Instant firstAt = Instant.parse("2026-08-28T00:00:00Z");
    UUID firstEvent = UUID.randomUUID();
    UUID secondEvent = UUID.randomUUID();
    notifications.recordEvent(firstEvent, "OrderCreated", 1, firstAt);
    notifications.recordEvent(secondEvent, "OrderCreated", 1, firstAt.plusSeconds(1));
    Notification first = notification(firstEvent, firstAt);
    Notification second = notification(secondEvent, firstAt.plusSeconds(1));
    notifications.save(first);
    notifications.save(second);

    List<Notification> page = notifications.findByUser(userId, false, null, null, 1);
    assertThat(page).containsExactly(second);
    assertThat(notifications.findByUser(userId, false, second.createdAt(), second.id(), 1))
        .containsExactly(first);
    assertThat(notifications.markRead(otherUserId, second.id(), Instant.now())).isFalse();
    assertThat(notifications.countUnread(userId)).isEqualTo(2);
  }

  private Notification notification(UUID eventId, Instant createdAt) {
    return new Notification(
        UUID.randomUUID(),
        userId,
        eventId,
        NotificationType.ORDER_CREATED,
        "订单已创建",
        "你的订单已创建。",
        "/orders/order-1",
        null,
        createdAt);
  }

  private void createUsers() {
    userId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'USER', ?, ?), (?, ?, ?, ?, 'USER', ?, ?)",
        userId,
        userId + "@example.com",
        "hash",
        "Notification User",
        now,
        now,
        otherUserId,
        otherUserId + "@example.com",
        "hash",
        "Other User",
        now,
        now);
  }
}
