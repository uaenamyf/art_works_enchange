package com.creatorhub.digitaldelivery.infrastructure.messaging;

import com.creatorhub.digitaldelivery.application.port.out.DownloadGrantRepository;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import com.creatorhub.outbox.application.port.out.OutboxEventHandler;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 撤回事件消费者；按资产版本条件撤销授权并失效未使用下载凭证，重复消费无副作用。 */
@Component
public class DigitalAssetRecallEventHandler implements OutboxEventHandler {

  private final EntitlementRepository entitlements;
  private final DownloadGrantRepository grants;

  public DigitalAssetRecallEventHandler(
      EntitlementRepository entitlements, DownloadGrantRepository grants) {
    this.entitlements = entitlements;
    this.grants = grants;
  }

  @Override
  public boolean supports(String eventType, int eventVersion) {
    return "DigitalAssetRecalled".equals(eventType) && eventVersion == 1;
  }

  @Override
  @Transactional
  public void handle(OutboxEvent event) {
    if (!supports(event.eventType(), event.eventVersion())) return;
    UUID assetVersionId = UUID.fromString(text(event, "assetVersionId"));
    String reason = text(event, "reasonCode");
    Instant revokedAt = event.occurredAt();
    entitlements.revokeAvailableByAssetVersion(assetVersionId, reason, revokedAt);
    grants.revokeByAssetVersion(assetVersionId, revokedAt);
  }

  private static String text(OutboxEvent event, String key) {
    Object value = event.payload().get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("Digital asset recall payload missing " + key);
    }
    return text;
  }
}
