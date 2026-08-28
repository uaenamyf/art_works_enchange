package com.creatorhub.notification.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证控制台邮件 stub 可调用且不会依赖真实 SMTP。 */
class ConsoleEmailDeliveryAdapterTest {

  @Test
  void acceptsEmailMessageWithoutExternalProvider() {
    EmailDeliveryPort adapter = new ConsoleEmailDeliveryAdapter();

    assertThatCode(
            () ->
                adapter.send(
                    new EmailDeliveryPort.EmailMessage(
                        UUID.randomUUID(), "creator@example.com", "作品已发布", "你的作品已经发布。")))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsIncompleteEmailMessage() {
    assertThatThrownBy(() -> new EmailDeliveryPort.EmailMessage(UUID.randomUUID(), "", "主题", "正文"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("recipientEmail is required");
  }
}
