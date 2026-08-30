package com.creatorhub.digitaldelivery.application.service;

import com.creatorhub.digitaldelivery.application.port.in.DigitalEntitlementUseCases;
import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数字授权应用服务；支付事件重复投递时按订单唯一授权返回已有结果。 */
@Service
public class DigitalEntitlementService implements DigitalEntitlementUseCases {

  private final EntitlementRepository entitlements;
  private final OutboxRepository outbox;

  public DigitalEntitlementService(EntitlementRepository entitlements) {
    this(entitlements, null);
  }

  @Autowired
  public DigitalEntitlementService(EntitlementRepository entitlements, OutboxRepository outbox) {
    this.entitlements = entitlements;
    this.outbox = outbox;
  }

  @Override
  @Transactional(readOnly = true)
  public EntitlementView getForBuyer(UUID buyerId, UUID orderId) {
    Entitlement entitlement =
        entitlements
            .findByOrderId(orderId)
            .filter(candidate -> candidate.buyerId().equals(buyerId))
            .orElseThrow(() -> new BusinessError("DIGITAL_ENTITLEMENT_NOT_FOUND", "Digital entitlement was not found"));
    return new EntitlementView(
        entitlement.id(),
        entitlement.orderId(),
        entitlement.artworkId(),
        entitlement.assetVersionId(),
        entitlement.status().name(),
        entitlement.grantedAt(),
        entitlement.revokedAt(),
        entitlement.createdAt(),
        entitlement.updatedAt());
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
    Entitlement granted =
        entitlements.grant(
            Entitlement.available(
                UUID.randomUUID(), orderId, buyerId, artworkId, assetVersionId, paidAt));
    if (outbox != null) {
      outbox.append(
          OutboxEvent.pending(
              UUID.nameUUIDFromBytes(
                  ("DigitalEntitlementGranted:" + paymentEventId).getBytes(StandardCharsets.UTF_8)),
              "DigitalEntitlementGranted",
              1,
              "Entitlement",
              granted.id(),
              0,
              paidAt,
              paymentEventId,
              java.util.Map.of(
                  "recipientId", granted.buyerId().toString(),
                  "orderId", granted.orderId().toString(),
                  "assetVersionId", granted.assetVersionId().toString(),
                  "entitlementId", granted.id().toString(),
                  "title", "数字资产已可下载",
                  "body", "你的数字资产已准备好，可以查看订单并下载。")));
    }
    return granted;
  }
}
