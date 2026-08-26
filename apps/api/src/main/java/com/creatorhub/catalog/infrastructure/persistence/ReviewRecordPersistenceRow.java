package com.creatorhub.catalog.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import java.time.Instant;
import java.util.UUID;

/** {@code artwork_review_records} 审核审计行，只在 infrastructure 层使用。 */
@TableName("artwork_review_records")
class ReviewRecordPersistenceRow {

  @TableId(type = IdType.INPUT)
  private UUID id;

  private UUID artworkId;
  private UUID reviewerId;
  private ArtworkStatus decision;
  private String reason;
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getArtworkId() {
    return artworkId;
  }

  public void setArtworkId(UUID artworkId) {
    this.artworkId = artworkId;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(UUID reviewerId) {
    this.reviewerId = reviewerId;
  }

  public ArtworkStatus getDecision() {
    return decision;
  }

  public void setDecision(ArtworkStatus decision) {
    this.decision = decision;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
