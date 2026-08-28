package com.creatorhub.catalog.application.port.in;

import com.creatorhub.catalog.domain.model.DigitalAssetUploadRequest;
import java.time.Instant;
import java.util.UUID;

/** 数字资产上传用例；上传 URL 仅用于私有对象，确认后进入扫描流程。 */
public interface DigitalAssetUploadUseCases {

  UploadView createUpload(UUID ownerId, UUID artworkId, DigitalAssetUploadRequest request);

  void confirmUpload(UUID ownerId, UUID versionId);

  record UploadView(
      UUID versionId,
      int version,
      String uploadUrl,
      String method,
      String contentType,
      Instant expiresAt) {}
}
