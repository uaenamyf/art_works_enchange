package com.creatorhub.catalog.application.port.out;

import com.creatorhub.catalog.domain.model.MediaUploadRequest;
import java.time.Instant;
import java.util.UUID;

/** 媒体存储输出端口，隔离 Catalog 用例与 MinIO/S3 SDK。 */
public interface MediaStoragePort {

  PresignedUpload presign(
      UUID mediaId, String objectKey, MediaUploadRequest request, Instant expiresAt);

  boolean exists(String objectKey);

  record PresignedUpload(String uploadUrl, String method, String contentType, Instant expiresAt) {}
}
