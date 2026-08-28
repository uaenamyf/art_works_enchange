package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.in.DigitalAssetRecallUseCase;
import com.creatorhub.catalog.application.port.out.DigitalAssetStore;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.domain.AppClock;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数字资产撤回应用服务；只允许 owner 撤回 READY 版本，并通过 Outbox 通知下游撤销授权。 */
@Service
@Transactional
public class DigitalAssetRecallService implements DigitalAssetRecallUseCase {

  private final DigitalAssetStore assets;
  private final OutboxRepository outbox;

  @Autowired
  public DigitalAssetRecallService(DigitalAssetStore assets, OutboxRepository outbox) {
    this.assets = assets;
    this.outbox = outbox;
  }

  @Override
  public DigitalAssetVersion recall(UUID ownerId, UUID versionId, String reason) {
    UUID artworkId = assets.findArtworkId(ownerId, versionId);
    java.time.Instant recalledAt = AppClock.now();
    DigitalAssetVersion recalled = assets.recall(ownerId, versionId, reason, recalledAt);
    outbox.append(
        OutboxEvent.pending(
            UUID.randomUUID(),
            "DigitalAssetRecalled",
            1,
            "DigitalAsset",
            recalled.digitalAssetId(),
            recalled.version(),
            recalledAt,
            UUID.randomUUID(),
            Map.of(
                "artworkId",
                artworkId.toString(),
                "recipientId",
                ownerId.toString(),
                "assetVersionId",
                recalled.id().toString(),
                "reasonCode",
                reason.trim(),
                "title",
                "数字资产已撤回",
                "body",
                "你的数字资产版本已撤回，新的下载授权将被停止。")));
    return recalled;
  }
}
