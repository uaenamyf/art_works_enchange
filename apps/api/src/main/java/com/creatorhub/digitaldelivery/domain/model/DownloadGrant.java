package com.creatorhub.digitaldelivery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 一次下载的短期授权凭证；数据库只保存摘要，原始 Token 不进入持久化或日志。 */
public record DownloadGrant(
    UUID id,
    UUID entitlementId,
    String tokenDigest,
    Instant expiresAt,
    Instant usedAt,
    Instant revokedAt,
    String requestIpHash,
    String userAgentHash,
    Instant createdAt) {

  public DownloadGrant {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(entitlementId, "entitlementId");
    tokenDigest = requiredDigest(tokenDigest);
    Objects.requireNonNull(expiresAt, "expiresAt");
    Objects.requireNonNull(createdAt, "createdAt");
    if (!expiresAt.isAfter(createdAt)) {
      throw new IllegalArgumentException("expiresAt must be after createdAt");
    }
  }

  public static DownloadGrant issue(
      UUID id,
      UUID entitlementId,
      String tokenDigest,
      Instant expiresAt,
      String requestIpHash,
      String userAgentHash,
      Instant createdAt) {
    return new DownloadGrant(
        id,
        entitlementId,
        tokenDigest,
        expiresAt,
        null,
        null,
        requestIpHash,
        userAgentHash,
        createdAt);
  }

  public boolean usableAt(Instant now) {
    return revokedAt == null && usedAt == null && expiresAt.isAfter(now);
  }

  public DownloadGrant markUsed(Instant at) {
    return new DownloadGrant(
        id,
        entitlementId,
        tokenDigest,
        expiresAt,
        Objects.requireNonNull(at, "at"),
        revokedAt,
        requestIpHash,
        userAgentHash,
        createdAt);
  }

  private static String requiredDigest(String value) {
    if (value == null || !value.matches("[0-9a-fA-F]{64}")) {
      throw new IllegalArgumentException("tokenDigest must be a hexadecimal digest");
    }
    return value.toLowerCase(java.util.Locale.ROOT);
  }
}
