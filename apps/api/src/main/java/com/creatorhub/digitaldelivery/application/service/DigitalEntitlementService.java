package com.creatorhub.digitaldelivery.application.service;

import com.creatorhub.digitaldelivery.application.port.in.DigitalEntitlementUseCases;
import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数字授权应用服务；支付事件重复投递时按订单唯一授权返回已有结果。 */
@Service
public class DigitalEntitlementService implements DigitalEntitlementUseCases {

  private final EntitlementRepository entitlements;

  public DigitalEntitlementService(EntitlementRepository entitlements) {
    this.entitlements = entitlements;
  }

  @Override
  @Transactional
  public Entitlement grantAfterPayment(
      UUID paymentEventId,
      UUID orderId,
      UUID buyerId,
      UUID artworkId,
      UUID assetVersionId,
      Instant paidAt) {
    Objects.requireNonNull(paymentEventId, "paymentEventId");
    Objects.requireNonNull(paidAt, "paidAt");
    return entitlements.grant(
        Entitlement.available(
            UUID.randomUUID(), orderId, buyerId, artworkId, assetVersionId, paidAt));
  }
}
