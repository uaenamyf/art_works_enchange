package com.creatorhub.catalog.application.port.out;

import java.time.Instant;

/** 数字资产私有对象存储端口；不向应用层暴露 S3/MinIO SDK 类型或长期下载地址。 */
public interface DigitalAssetStoragePort {

  PresignedUpload presignUpload(
      String objectKey, String contentType, long sizeBytes, Instant expiresAt);

  boolean exists(String objectKey);

  record PresignedUpload(String uploadUrl, String method, String contentType, Instant expiresAt) {}
}
