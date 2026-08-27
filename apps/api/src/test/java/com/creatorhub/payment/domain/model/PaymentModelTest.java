package com.creatorhub.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.sharedkernel.domain.Money;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证支付领域对象的必填字段、Provider payload 不可变性和通知金额约束。 */
class PaymentModelTest {

  @Test
  void paymentSessionCopiesClientPayload() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("clientSecret", "opaque-client-value");

    PaymentSession session =
        new PaymentSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            PaymentProvider.STRIPE,
            PaymentStatus.REQUIRES_ACTION,
            payload);

    payload.put("unexpected", "not-visible");

    assertThat(session.clientPayload()).containsEntry("clientSecret", "opaque-client-value");
    assertThat(session.clientPayload()).doesNotContainKey("unexpected");
    assertThatThrownBy(() -> session.clientPayload().put("another", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void paymentNotificationRequiresProviderEventId() {
    assertThatThrownBy(
            () ->
                new PaymentNotification(
                    PaymentProvider.WECHAT,
                    " ",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "CH202608270001",
                    Money.of("10.00", "CNY"),
                    PaymentStatus.SUCCEEDED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerEventId is required");
  }

  @Test
  void paymentNotificationRetainsCurrencyAndAmount() {
    PaymentNotification notification =
        new PaymentNotification(
            PaymentProvider.ALIPAY,
            "event-1",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "CH202608270001",
            Money.of("10", "CNY"),
            PaymentStatus.PROCESSING);

    assertThat(notification.amount()).isEqualTo(Money.of("10.00", "CNY"));
    assertThat(notification.status()).isEqualTo(PaymentStatus.PROCESSING);
  }
}
