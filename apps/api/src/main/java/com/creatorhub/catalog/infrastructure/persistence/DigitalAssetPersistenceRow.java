package com.creatorhub.catalog.infrastructure.persistence;

import com.creatorhub.catalog.domain.model.DigitalAssetScanStatus;
import java.time.Instant;
import java.util.UUID;

/** 数字资产版本持久化行，仅在 Catalog infrastructure 层使用。 */
class DigitalAssetPersistenceRow {

  private UUID id;
  private UUID digitalAssetId;
  private int version;
  private String fileName;
  private String objectKey;
  private String contentType;
  private long sizeBytes;
  private String sha256;
  private DigitalAssetScanStatus scanStatus;
  private String scanError;
  private Instant createdAt;
  private Instant scannedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getDigitalAssetId() {
    return digitalAssetId;
  }

  public void setDigitalAssetId(UUID digitalAssetId) {
    this.digitalAssetId = digitalAssetId;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public void setObjectKey(String objectKey) {
    this.objectKey = objectKey;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

  public DigitalAssetScanStatus getScanStatus() {
    return scanStatus;
  }

  public void setScanStatus(DigitalAssetScanStatus scanStatus) {
    this.scanStatus = scanStatus;
  }

  public String getScanError() {
    return scanError;
  }

  public void setScanError(String scanError) {
    this.scanError = scanError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getScannedAt() {
    return scannedAt;
  }

  public void setScannedAt(Instant scannedAt) {
    this.scannedAt = scannedAt;
  }
}
