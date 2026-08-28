package com.creatorhub.digitaldelivery.infrastructure.persistence;

import com.creatorhub.digitaldelivery.domain.model.EntitlementStatus;
import java.time.Instant;
import java.util.UUID;

/** digital_entitlements 持久化行，仅在 Digital Delivery infrastructure 层使用。 */
class EntitlementPersistenceRow {

  private UUID id;
  private UUID orderId;
  private UUID buyerId;
  private UUID artworkId;
  private UUID assetVersionId;
  private EntitlementStatus status;
  private Instant grantedAt;
  private Instant revokedAt;
  private String revokeReason;
  private Instant createdAt;
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }

  public UUID getBuyerId() {
    return buyerId;
  }

  public void setBuyerId(UUID buyerId) {
    this.buyerId = buyerId;
  }

  public UUID getArtworkId() {
    return artworkId;
  }

  public void setArtworkId(UUID artworkId) {
    this.artworkId = artworkId;
  }

  public UUID getAssetVersionId() {
    return assetVersionId;
  }

  public void setAssetVersionId(UUID assetVersionId) {
    this.assetVersionId = assetVersionId;
  }

  public EntitlementStatus getStatus() {
    return status;
  }

  public void setStatus(EntitlementStatus status) {
    this.status = status;
  }

  public Instant getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(Instant grantedAt) {
    this.grantedAt = grantedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  public String getRevokeReason() {
    return revokeReason;
  }

  public void setRevokeReason(String revokeReason) {
    this.revokeReason = revokeReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
