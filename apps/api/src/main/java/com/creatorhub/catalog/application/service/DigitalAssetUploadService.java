package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.in.DigitalAssetUploadUseCases;
import com.creatorhub.catalog.application.port.out.DigitalAssetStoragePort;
import com.creatorhub.catalog.application.port.out.DigitalAssetStore;
import com.creatorhub.catalog.domain.model.DigitalAssetUploadRequest;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数字资产上传应用服务；数据库记录和私有对象存储通过 owner 校验与确认步骤关联。 */
@Service
@Transactional
public class DigitalAssetUploadService implements DigitalAssetUploadUseCases {

  private final DigitalAssetStore assets;
  private final DigitalAssetStoragePort storage;
  private final long uploadTtlSeconds;

  public DigitalAssetUploadService(
      DigitalAssetStore assets,
      DigitalAssetStoragePort storage,
      @Value("${creatorhub.storage.upload-url-ttl-seconds:900}") long uploadTtlSeconds) {
    this.assets = assets;
    this.storage = storage;
    this.uploadTtlSeconds = uploadTtlSeconds;
  }

  @Override
  public UploadView createUpload(UUID ownerId, UUID artworkId, DigitalAssetUploadRequest request) {
    DigitalAssetVersion version =
        assets.createPendingVersion(
            ownerId,
            artworkId,
            request.fileName(),
            request.contentType(),
            request.sizeBytes(),
            request.sha256());
    Instant expiresAt = AppClock.now().plus(Duration.ofSeconds(uploadTtlSeconds));
    DigitalAssetStoragePort.PresignedUpload upload =
        storage.presignUpload(
            version.objectKey(), request.contentType(), request.sizeBytes(), expiresAt);
    return new UploadView(
        version.id(),
        version.version(),
        upload.uploadUrl(),
        upload.method(),
        upload.contentType(),
        upload.expiresAt());
  }

  @Override
  public void confirmUpload(UUID ownerId, UUID versionId) {
    DigitalAssetVersion version = assets.findVersion(ownerId, versionId);
    if (!storage.exists(version.objectKey())) {
      throw new BusinessError(
          "DIGITAL_ASSET_OBJECT_NOT_FOUND", "Digital asset object was not found");
    }
    assets.confirmUpload(ownerId, versionId);
  }
}
