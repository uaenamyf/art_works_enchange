package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.in.ArtworkReviewUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import com.creatorhub.catalog.application.port.out.ReviewRecordStore;
import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.catalog.domain.repository.ArtworkRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Artwork 审核应用服务，统一执行审核员授权后的聚合状态变更。 */
@Service
@Transactional
public class ArtworkReviewService implements ArtworkReviewUseCases {

  private final ArtworkRepository artworks;
  private final ReviewRecordStore reviewRecords;
  private final OutboxRepository outbox;

  public ArtworkReviewService(ArtworkRepository artworks, ReviewRecordStore reviewRecords) {
    this(artworks, reviewRecords, null);
  }

  @Autowired
  public ArtworkReviewService(
      ArtworkRepository artworks, ReviewRecordStore reviewRecords, OutboxRepository outbox) {
    this.artworks = artworks;
    this.reviewRecords = reviewRecords;
    this.outbox = outbox;
  }

  @Override
  public ArtworkView approve(UUID reviewerId, UUID artworkId, String reason) {
    Artwork artwork = find(artworkId);
    String normalizedReason = reason(reason);
    artwork.approve(AppClock.now());
    Artwork saved = artworks.save(artwork);
    reviewRecords.append(
        saved.id().value(), reviewerId, saved.status(), normalizedReason, AppClock.now());
    publishReviewEvent(saved, normalizedReason);
    return view(saved);
  }

  @Override
  public ArtworkView reject(UUID reviewerId, UUID artworkId, String reason) {
    Artwork artwork = find(artworkId);
    String normalizedReason = reason(reason);
    artwork.reject(AppClock.now());
    Artwork saved = artworks.save(artwork);
    reviewRecords.append(
        saved.id().value(), reviewerId, saved.status(), normalizedReason, AppClock.now());
    publishReviewEvent(saved, normalizedReason);
    return view(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReviewView> listForOwner(UUID ownerId, UUID artworkId) {
    Artwork artwork =
        artworks
            .findByOwnerId(com.creatorhub.sharedkernel.UserId.of(ownerId), ArtworkId.of(artworkId))
            .orElseThrow(() -> new BusinessError("ARTWORK_NOT_FOUND", "Artwork not found"));
    return reviewRecords.findByArtworkId(artwork.id().value()).stream()
        .map(
            record ->
                new ReviewView(
                    record.id(),
                    record.artworkId(),
                    record.reviewerId(),
                    record.decision(),
                    record.reason(),
                    record.createdAt()))
        .toList();
  }

  private void publishReviewEvent(Artwork artwork, String reason) {
    if (outbox == null) return;
    boolean approved =
        artwork.status() == com.creatorhub.catalog.domain.model.ArtworkStatus.APPROVED;
    outbox.append(
        OutboxEvent.pending(
            UUID.randomUUID(),
            approved ? "ArtworkReviewApproved" : "ArtworkReviewRejected",
            1,
            "Artwork",
            artwork.id().value(),
            artwork.version(),
            AppClock.now(),
            UUID.randomUUID(),
            java.util.Map.of(
                "recipientId",
                artwork.ownerId().value().toString(),
                "artworkId",
                artwork.id().value().toString(),
                "title",
                approved ? "作品审核通过" : "作品需要修改",
                "body",
                reason)));
  }

  private Artwork find(UUID artworkId) {
    return artworks
        .findById(ArtworkId.of(artworkId))
        .orElseThrow(() -> new BusinessError("ARTWORK_NOT_FOUND", "Artwork not found"));
  }

  private static String reason(String reason) {
    String normalized = java.util.Objects.requireNonNull(reason, "reason").trim();
    if (normalized.isEmpty() || normalized.length() > 2000) {
      throw new BusinessError("REVIEW_REASON_INVALID", "Review reason is invalid");
    }
    return normalized;
  }

  private static ArtworkView view(Artwork artwork) {
    return new ArtworkView(
        artwork.id().value(),
        artwork.creatorId(),
        artwork.title(),
        artwork.description(),
        artwork.category(),
        artwork.price().amount(),
        artwork.price().currency().getCurrencyCode(),
        artwork.stock(),
        artwork.mediaIds(),
        artwork.status(),
        artwork.productType(),
        artwork.version(),
        0,
        false);
  }
}
