package com.creatorhub.catalog.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** 数字资产不可变版本；文件元数据和扫描结果一经写入只能通过新版本演进。 */
public record DigitalAssetVersion(
    UUID id,
    UUID digitalAssetId,
    int version,
    String fileName,
    String objectKey,
    String contentType,
    long sizeBytes,
    String sha256,
    DigitalAssetScanStatus scanStatus,
    String scanError,
    Instant createdAt,
    Instant scannedAt) {

  public DigitalAssetVersion {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(digitalAssetId, "digitalAssetId");
    if (version < 1) throw new IllegalArgumentException("version must be positive");
    fileName = required(fileName, "fileName");
    objectKey = required(objectKey, "objectKey");
    contentType = required(contentType, "contentType").toLowerCase(Locale.ROOT);
    if (sizeBytes <= 0) throw new IllegalArgumentException("sizeBytes must be positive");
    sha256 = required(sha256, "sha256").toLowerCase(Locale.ROOT);
    if (!sha256.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("sha256 must be a hexadecimal digest");
    }
    Objects.requireNonNull(scanStatus, "scanStatus");
    Objects.requireNonNull(createdAt, "createdAt");
  }

  /** 新文件默认进入待扫描状态，防止上传完成后直接获得下载权限。 */
  public static DigitalAssetVersion pendingScan(
      UUID id,
      UUID digitalAssetId,
      int version,
      String fileName,
      String objectKey,
      String contentType,
      long sizeBytes,
      String sha256,
      Instant createdAt) {
    return new DigitalAssetVersion(
        id,
        digitalAssetId,
        version,
        fileName,
        objectKey,
        contentType,
        sizeBytes,
        sha256,
        DigitalAssetScanStatus.PENDING_SCAN,
        null,
        createdAt,
        null);
  }

  private static String required(String value, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException(name + " is required");
    return normalized;
  }
}
