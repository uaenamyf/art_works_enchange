package com.creatorhub.ordering.infrastructure.persistence;

import com.creatorhub.ordering.domain.repository.PaymentEventConsumptionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 支付事件消费记录适配器；不向 Ordering 应用层泄漏 MyBatis 类型。 */
@Repository
public class PaymentEventConsumptionRepositoryAdapter implements PaymentEventConsumptionRepository {

  private final PaymentEventConsumptionMapper mapper;

  public PaymentEventConsumptionRepositoryAdapter(PaymentEventConsumptionMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean recordPaymentSucceeded(UUID eventId, UUID orderId, Instant consumedAt) {
    return mapper.recordPaymentSucceeded(eventId, orderId, consumedAt) == 1;
  }
}
