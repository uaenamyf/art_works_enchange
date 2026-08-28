package com.creatorhub.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证通知只能使用相对链接，并支持幂等安全的已读状态转换。 */
class NotificationTest {

  @Test
  void notificationAcceptsRelativeLinkAndMarksRead() {
    Notification notification = notification("/orders/" + UUID.randomUUID());
    Instant readAt = Instant.parse("2026-08-28T01:00:00Z");

    Notification read = notification.markRead(readAt);

    assertThat(read.readAt()).isEqualTo(readAt);
    assertThat(read.link()).startsWith("/orders/");
  }

  @Test
  void notificationRejectsExternalLink() {
    assertThatThrownBy(() -> notification("https://example.com/phishing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("link must be a relative path");
  }

  private static Notification notification(String link) {
    return new Notification(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        NotificationType.DIGITAL_ENTITLEMENT_GRANTED,
        "数字资产已可下载",
        "你的订单已准备好下载。",
        link,
        null,
        Instant.parse("2026-08-28T00:00:00Z"));
  }
}
