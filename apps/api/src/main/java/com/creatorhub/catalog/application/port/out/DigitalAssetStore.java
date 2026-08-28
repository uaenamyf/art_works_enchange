package com.creatorhub.catalog.application.port.out;

import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import java.util.UUID;

/** 数字资产输出端口；应用层通过它管理版本，不能直接依赖对象存储或扫描 SDK。 */
public interface DigitalAssetStore {

  DigitalAssetVersion createPendingVersion(
      UUID ownerId,
      UUID artworkId,
      String fileName,
      String contentType,
      long sizeBytes,
      String sha256);

  DigitalAssetVersion findVersion(UUID ownerId, UUID versionId);

  DigitalAssetVersion findVersion(UUID versionId);

  DigitalAssetVersion markScanStarted(UUID versionId);

  DigitalAssetVersion confirmUpload(UUID ownerId, UUID versionId);

  DigitalAssetVersion markScanPassed(UUID versionId, java.time.Instant scannedAt);

  DigitalAssetVersion quarantine(UUID versionId, String reason, java.time.Instant scannedAt);

  DigitalAssetVersion recall(
      UUID ownerId, UUID versionId, String reason, java.time.Instant recalledAt);

  UUID findArtworkId(UUID ownerId, UUID versionId);
}
