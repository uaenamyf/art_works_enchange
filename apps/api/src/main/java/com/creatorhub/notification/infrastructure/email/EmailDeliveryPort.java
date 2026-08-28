package com.creatorhub.notification.infrastructure.email;

import java.util.Objects;
import java.util.UUID;

/** 邮件通道适配器端口；Notification 领域不依赖 SMTP 或第三方邮件 SDK。 */
public interface EmailDeliveryPort {

  void send(EmailMessage message);

  /** 邮件命令只携带投递所需字段，禁止放入下载 Token、对象键或支付机密。 */
  record EmailMessage(UUID messageId, String recipientEmail, String subject, String body) {
    public EmailMessage {
      Objects.requireNonNull(messageId, "messageId");
      requireText(recipientEmail, "recipientEmail");
      requireText(subject, "subject");
      requireText(body, "body");
    }

    private static void requireText(String value, String name) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " is required");
      }
    }
  }
}
