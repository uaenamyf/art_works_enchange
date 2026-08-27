package com.creatorhub.ordering.infrastructure.scheduler;

import com.creatorhub.ordering.application.port.in.OrderUseCases;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时关闭过期待支付订单；状态条件更新和库存释放由 Ordering 应用服务保证幂等。 */
@Component
public class OrderExpiryScheduler {

  private final OrderUseCases orders;

  public OrderExpiryScheduler(OrderUseCases orders) {
    this.orders = orders;
  }

  @Scheduled(fixedDelayString = "${creatorhub.order.expiry-scan-delay-ms:30000}")
  public void expirePendingOrders() {
    orders.expirePendingOrders(Instant.now(), 100);
  }
}
