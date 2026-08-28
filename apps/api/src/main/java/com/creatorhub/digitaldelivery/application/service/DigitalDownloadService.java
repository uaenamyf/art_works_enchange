package com.creatorhub.digitaldelivery.application.service;

import com.creatorhub.digitaldelivery.application.port.out.DigitalAssetPrivateStoragePort;
import com.creatorhub.digitaldelivery.application.port.out.DigitalDownloadAsset;
import com.creatorhub.digitaldelivery.application.port.out.DownloadGrantRepository;
import com.creatorhub.digitaldelivery.domain.model.DownloadGrant;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 数字下载应用服务；只有买家有效授权和 READY 资产同时满足时才签发短期下载地址。 */
@Service
public class DigitalDownloadService {

  private final EntitlementRepository entitlements;
  private final DigitalAssetPrivateStoragePort storage;
  private final DownloadGrantRepository grants;
  private final long downloadTtlSeconds;

  public DigitalDownloadService(
      EntitlementRepository entitlements,
      DigitalAssetPrivateStoragePort storage,
      DownloadGrantRepository grants,
      @Value("${creatorhub.storage.download-url-ttl-seconds:300}") long downloadTtlSeconds) {
    this.entitlements = entitlements;
    this.storage = storage;
    this.grants = grants;
    this.downloadTtlSeconds = downloadTtlSeconds;
  }

  public DownloadView createGrant(UUID buyerId, UUID orderId) {
    DigitalDownloadAsset asset =
        entitlements
            .findDownloadAsset(buyerId, orderId)
            .orElseThrow(
                () ->
                    new BusinessError(
                        "DIGITAL_DOWNLOAD_NOT_AVAILABLE", "Digital download is not available"));
    Instant expiresAt = Instant.now().plus(Duration.ofSeconds(downloadTtlSeconds));
    Instant createdAt = Instant.now();
    String tokenDigest = digest(UUID.randomUUID().toString());
    DownloadGrant grant =
        DownloadGrant.issue(
            UUID.randomUUID(),
            asset.entitlementId(),
            tokenDigest,
            expiresAt,
            null,
            null,
            createdAt);
    grants.save(grant);
    String url = storage.presignDownload(asset.objectKey(), asset.contentType(), expiresAt);
    return new DownloadView(
        grant.id(),
        asset.fileName(),
        asset.contentType(),
        asset.sizeBytes(),
        asset.version(),
        url,
        expiresAt);
  }

  public record DownloadView(
      UUID grantId,
      String fileName,
      String contentType,
      long sizeBytes,
      int version,
      String downloadUrl,
      Instant expiresAt) {}

  private static String digest(String value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
