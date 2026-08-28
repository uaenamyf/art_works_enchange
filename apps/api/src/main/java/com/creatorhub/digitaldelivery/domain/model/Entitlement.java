package com.creatorhub.digitaldelivery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 买家对数字资产版本的长期访问授权，不保存短期下载 URL 或对象存储凭证。 */
public final class Entitlement {

  private final UUID id;
  private final UUID orderId;
  private final UUID buyerId;
  private final UUID artworkId;
  private final UUID assetVersionId;
  private final Instant createdAt;
  private EntitlementStatus status;
  private Instant grantedAt;
  private Instant revokedAt;
  private String revokeReason;
  private Instant updatedAt;

  private Entitlement(
      UUID id, UUID orderId, UUID buyerId, UUID artworkId, UUID assetVersionId, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.orderId = Objects.requireNonNull(orderId, "orderId");
    this.buyerId = Objects.requireNonNull(buyerId, "buyerId");
    this.artworkId = Objects.requireNonNull(artworkId, "artworkId");
    this.assetVersionId = Objects.requireNonNull(assetVersionId, "assetVersionId");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = createdAt;
    this.status = EntitlementStatus.PENDING;
  }

  public static Entitlement pending(
      UUID id, UUID orderId, UUID buyerId, UUID artworkId, UUID assetVersionId, Instant createdAt) {
    return new Entitlement(id, orderId, buyerId, artworkId, assetVersionId, createdAt);
  }

  public static Entitlement reconstitute(
      UUID id,
      UUID orderId,
      UUID buyerId,
      UUID artworkId,
      UUID assetVersionId,
      EntitlementStatus status,
      Instant grantedAt,
      Instant revokedAt,
      String revokeReason,
      Instant createdAt,
      Instant updatedAt) {
    Entitlement entitlement = pending(id, orderId, buyerId, artworkId, assetVersionId, createdAt);
    entitlement.status = Objects.requireNonNull(status, "status");
    entitlement.grantedAt = grantedAt;
    entitlement.revokedAt = revokedAt;
    entitlement.revokeReason = revokeReason;
    entitlement.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    return entitlement;
  }

  public void makeAvailable(Instant at) {
    if (status != EntitlementStatus.PENDING) {
      throw new IllegalStateException("Entitlement cannot become available from " + status);
    }
    status = EntitlementStatus.AVAILABLE;
    grantedAt = Objects.requireNonNull(at, "at");
    updatedAt = at;
  }

  public void revoke(String reason, Instant at) {
    if (status == EntitlementStatus.REVOKED) return;
    if (status != EntitlementStatus.AVAILABLE) {
      throw new IllegalStateException("Entitlement cannot be revoked from " + status);
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("revoke reason is required");
    }
    status = EntitlementStatus.REVOKED;
    revokeReason = reason.trim();
    revokedAt = Objects.requireNonNull(at, "at");
    updatedAt = at;
  }

  public UUID id() {
    return id;
  }

  public UUID orderId() {
    return orderId;
  }

  public UUID buyerId() {
    return buyerId;
  }

  public UUID artworkId() {
    return artworkId;
  }

  public UUID assetVersionId() {
    return assetVersionId;
  }

  public EntitlementStatus status() {
    return status;
  }

  public Instant grantedAt() {
    return grantedAt;
  }

  public Instant revokedAt() {
    return revokedAt;
  }

  public String revokeReason() {
    return revokeReason;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
