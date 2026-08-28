package com.creatorhub.digitaldelivery.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

/** digital_download_grants 持久化行，仅在 Digital Delivery infrastructure 层使用。 */
class DownloadGrantPersistenceRow {

  private UUID id;
  private UUID entitlementId;
  private String tokenDigest;
  private Instant expiresAt;
  private Instant usedAt;
  private Instant revokedAt;
  private String requestIpHash;
  private String userAgentHash;
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEntitlementId() {
    return entitlementId;
  }

  public void setEntitlementId(UUID entitlementId) {
    this.entitlementId = entitlementId;
  }

  public String getTokenDigest() {
    return tokenDigest;
  }

  public void setTokenDigest(String tokenDigest) {
    this.tokenDigest = tokenDigest;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  public String getRequestIpHash() {
    return requestIpHash;
  }

  public void setRequestIpHash(String requestIpHash) {
    this.requestIpHash = requestIpHash;
  }

  public String getUserAgentHash() {
    return userAgentHash;
  }

  public void setUserAgentHash(String userAgentHash) {
    this.userAgentHash = userAgentHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
