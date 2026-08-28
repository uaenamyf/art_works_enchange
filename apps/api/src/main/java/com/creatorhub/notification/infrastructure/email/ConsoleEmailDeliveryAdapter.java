package com.creatorhub.notification.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** MVP 邮件适配器；只输出脱敏元数据，后续可替换为真实邮件供应商而不改业务层。 */
@Component
public class ConsoleEmailDeliveryAdapter implements EmailDeliveryPort {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleEmailDeliveryAdapter.class);

  @Override
  public void send(EmailMessage message) {
    LOGGER.info(
        "Email delivery stub invoked: messageId={}, recipient={}, subjectLength={}, bodyLength={}",
        message.messageId(),
        maskRecipient(message.recipientEmail()),
        message.subject().length(),
        message.body().length());
  }

  private static String maskRecipient(String email) {
    int separator = email.indexOf('@');
    if (separator <= 1) return "***";
    return email.charAt(0) + "***" + email.substring(separator);
  }
}
