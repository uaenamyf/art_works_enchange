package com.creatorhub.catalog.infrastructure.persistence;

import com.creatorhub.catalog.application.port.out.ReviewRecordStore;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 审核记录持久化适配器，只追加审计事实，不向上层暴露 PersistenceRow。 */
@Repository
public class ReviewRecordStoreAdapter implements ReviewRecordStore {

  private final ReviewRecordMapper mapper;

  public ReviewRecordStoreAdapter(ReviewRecordMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void append(
      UUID artworkId, UUID reviewerId, ArtworkStatus decision, String reason, Instant createdAt) {
    ReviewRecordPersistenceRow row = new ReviewRecordPersistenceRow();
    row.setId(UUID.randomUUID());
    row.setArtworkId(artworkId);
    row.setReviewerId(reviewerId);
    row.setDecision(decision);
    row.setReason(reason);
    row.setCreatedAt(createdAt);
    if (mapper.insert(row) != 1) {
      throw new IllegalStateException("Review record was not persisted");
    }
  }
}
