package com.creatorhub.ordering.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 验证订单价格快照、地址校验和支付/履约状态机不变量。 */
class OrderTest {

  private static final Instant CREATED_AT = Instant.parse("2026-08-27T00:00:00Z");
  private static final ShippingAddress ADDRESS =
      new ShippingAddress(
          "Alice", "1 Main Street", null, "Shanghai", null, "200000", "CN", "13800138000");

  @Test
  void createsPendingOrderWithFrozenSubtotal() {
    Order order = order();

    assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    assertThat(order.subtotal()).isEqualTo(Money.of("198.00", "CNY"));
    assertThat(order.lines()).hasSize(1);
  }

  @Test
  void rejectsIllegalStatusTransition() {
    Order order = order();

    assertThatThrownBy(() -> order.startProcessing(CREATED_AT.plusSeconds(1)))
        .isInstanceOf(IllegalStateException.class);
    order.markPaid(CREATED_AT.plusSeconds(1));
    order.startProcessing(CREATED_AT.plusSeconds(2));
    assertThat(order.status()).isEqualTo(OrderStatus.PROCESSING);
  }

  @Test
  void recordsShippingTrackingWhenMarkingOrderShipped() {
    Order order = order();
    order.markPaid(CREATED_AT.plusSeconds(1));
    order.startProcessing(CREATED_AT.plusSeconds(2));

    order.markShipped(CREATED_AT.plusSeconds(3), "SF123456", "顺丰");

    assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
    assertThat(order.trackingNumber()).isEqualTo("SF123456");
    assertThat(order.carrier()).isEqualTo("顺丰");
  }

  @Test
  void rejectsOversizedShippingTracking() {
    Order order = order();
    order.markPaid(CREATED_AT.plusSeconds(1));
    order.startProcessing(CREATED_AT.plusSeconds(2));

    assertThatThrownBy(
            () -> order.markShipped(CREATED_AT.plusSeconds(3), "x".repeat(101), "Carrier"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidAddressAndQuantity() {
    assertThatThrownBy(
            () ->
                new ShippingAddress(
                    "", "1 Main Street", null, "Shanghai", null, "200000", "CN", "13800138000"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderLine(ArtworkId.random(), "Vase", Money.of("99.00", "CNY"), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Order order() {
    return Order.create(
        OrderId.random(),
        UserId.random(),
        "CH202608270001",
        new OrderLine(ArtworkId.random(), "Vase", Money.of("99.00", "CNY"), 2),
        ADDRESS,
        CREATED_AT,
        CREATED_AT.plusSeconds(1800));
  }
}
