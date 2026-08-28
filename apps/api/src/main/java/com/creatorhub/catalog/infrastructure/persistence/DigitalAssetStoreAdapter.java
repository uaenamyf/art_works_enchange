package com.creatorhub.catalog.infrastructure.persistence;

import com.creatorhub.catalog.application.port.out.DigitalAssetStore;
import com.creatorhub.catalog.domain.model.DigitalAssetScanStatus;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 数字资产 PostgreSQL 适配器；只保存私有对象元数据，原始文件由对象存储适配器管理。 */
@Repository
public class DigitalAssetStoreAdapter implements DigitalAssetStore {

  private final DigitalAssetMapper mapper;

  public DigitalAssetStoreAdapter(DigitalAssetMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public DigitalAssetVersion createPendingVersion(
      UUID ownerId,
      UUID artworkId,
      String fileName,
      String contentType,
      long sizeBytes,
      String sha256) {
    Instant now = AppClock.now();
    UUID assetId = mapper.findAssetIdByArtwork(artworkId);
    if (assetId == null) {
      assetId = UUID.randomUUID();
      if (mapper.insertAssetIfAbsent(assetId, artworkId, ownerId, now) != 1) {
        throw new BusinessError("DIGITAL_ARTWORK_NOT_FOUND", "Digital artwork was not found");
      }
    }
    DigitalAssetPersistenceRow row = new DigitalAssetPersistenceRow();
    row.setId(UUID.randomUUID());
    row.setDigitalAssetId(assetId);
    row.setVersion(mapper.nextVersion(assetId));
    row.setFileName(fileName);
    row.setObjectKey("private/digital/" + assetId + "/" + row.getVersion() + "/" + row.getId());
    row.setContentType(contentType);
    row.setSizeBytes(sizeBytes);
    row.setSha256(sha256);
    row.setScanStatus(DigitalAssetScanStatus.PENDING_SCAN);
    row.setCreatedAt(now);
    if (mapper.insertVersion(row) != 1) {
      throw new IllegalStateException("Digital asset version insert failed");
    }
    return toDomain(row);
  }

  @Override
  public DigitalAssetVersion markScanStarted(UUID versionId) {
    if (mapper.markScanning(versionId) != 1) {
      throw new BusinessError(
          "DIGITAL_ASSET_SCAN_STATE_INVALID", "Digital asset cannot start scanning");
    }
    return current(versionId);
  }

  @Override
  @Transactional
  public DigitalAssetVersion markScanPassed(UUID versionId, Instant scannedAt) {
    if (mapper.markReady(versionId, scannedAt) != 1) {
      throw new BusinessError(
          "DIGITAL_ASSET_SCAN_STATE_INVALID", "Digital asset cannot be marked ready");
    }
    DigitalAssetPersistenceRow row = currentRow(versionId);
    mapper.markAssetReady(row.getDigitalAssetId(), row.getVersion());
    return toDomain(row);
  }

  @Override
  public DigitalAssetVersion quarantine(UUID versionId, String reason, Instant scannedAt) {
    if (mapper.quarantine(versionId, reason, scannedAt) != 1) {
      throw new BusinessError(
          "DIGITAL_ASSET_SCAN_STATE_INVALID", "Digital asset cannot be quarantined");
    }
    return current(versionId);
  }

  private DigitalAssetVersion current(UUID versionId) {
    return toDomain(currentRow(versionId));
  }

  private DigitalAssetPersistenceRow currentRow(UUID versionId) {
    DigitalAssetPersistenceRow row = mapper.findVersion(versionId);
    if (row == null)
      throw new BusinessError(
          "DIGITAL_ASSET_VERSION_NOT_FOUND", "Digital asset version was not found");
    return row;
  }

  private static DigitalAssetVersion toDomain(DigitalAssetPersistenceRow row) {
    return new DigitalAssetVersion(
        row.getId(),
        row.getDigitalAssetId(),
        row.getVersion(),
        row.getFileName(),
        row.getObjectKey(),
        row.getContentType(),
        row.getSizeBytes(),
        row.getSha256(),
        row.getScanStatus(),
        row.getScanError(),
        row.getCreatedAt(),
        row.getScannedAt());
  }
}
