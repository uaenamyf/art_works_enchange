package com.creatorhub.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.catalog.application.port.out.ReviewRecordStore;
import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import com.creatorhub.catalog.domain.repository.ArtworkRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证审核服务会记录审核事实，并在无效理由时不改变作品状态。 */
class ArtworkReviewServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

  @Test
  void approvesArtworkAndAppendsAuditRecord() {
    InMemoryArtworkRepository artworks = new InMemoryArtworkRepository();
    RecordingReviewRecords records = new RecordingReviewRecords();
    Artwork artwork = pendingArtwork();
    artworks.save(artwork);
    ArtworkReviewService service = new ArtworkReviewService(artworks, records);
    UUID reviewerId = UUID.randomUUID();

    var result = service.approve(reviewerId, artwork.id().value(), "meets policy");

    assertThat(result.status()).isEqualTo(ArtworkStatus.APPROVED);
    assertThat(records.entries)
        .containsExactly(
            new Entry(artwork.id().value(), reviewerId, ArtworkStatus.APPROVED, "meets policy"));
  }

  @Test
  void rejectsArtworkAndAppendsAuditRecord() {
    InMemoryArtworkRepository artworks = new InMemoryArtworkRepository();
    RecordingReviewRecords records = new RecordingReviewRecords();
    Artwork artwork = pendingArtwork();
    artworks.save(artwork);
    ArtworkReviewService service = new ArtworkReviewService(artworks, records);
    UUID reviewerId = UUID.randomUUID();

    var result = service.reject(reviewerId, artwork.id().value(), "missing rights proof");

    assertThat(result.status()).isEqualTo(ArtworkStatus.REJECTED);
    assertThat(records.entries)
        .containsExactly(
            new Entry(
                artwork.id().value(), reviewerId, ArtworkStatus.REJECTED, "missing rights proof"));
  }

  @Test
  void invalidReasonDoesNotChangeArtworkOrAppendRecord() {
    InMemoryArtworkRepository artworks = new InMemoryArtworkRepository();
    RecordingReviewRecords records = new RecordingReviewRecords();
    Artwork artwork = pendingArtwork();
    artworks.save(artwork);
    ArtworkReviewService service = new ArtworkReviewService(artworks, records);

    assertThatThrownBy(() -> service.approve(UUID.randomUUID(), artwork.id().value(), "  "))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("REVIEW_REASON_INVALID");
    assertThat(artwork.status()).isEqualTo(ArtworkStatus.PENDING_REVIEW);
    assertThat(records.entries).isEmpty();
  }

  private static Artwork pendingArtwork() {
    Artwork artwork =
        Artwork.draft(
            ArtworkId.random(),
            UserId.random(),
            UUID.randomUUID(),
            "Test artwork",
            "A sufficiently long description for the artwork.",
            "ART",
            Money.of("99.00", "USD"),
            2,
            List.of(UUID.randomUUID()),
            NOW);
    artwork.submitForReview(NOW);
    return artwork;
  }

  private record Entry(UUID artworkId, UUID reviewerId, ArtworkStatus decision, String reason) {}

  private static final class RecordingReviewRecords implements ReviewRecordStore {
    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void append(
        UUID artworkId, UUID reviewerId, ArtworkStatus decision, String reason, Instant createdAt) {
      entries.add(new Entry(artworkId, reviewerId, decision, reason));
    }

    @Override
    public List<ReviewRecord> findByArtworkId(UUID artworkId) {
      return List.of();
    }
  }

  private static final class InMemoryArtworkRepository implements ArtworkRepository {
    private Artwork artwork;

    @Override
    public Optional<Artwork> findById(ArtworkId id) {
      return artwork != null && artwork.id().equals(id) ? Optional.of(artwork) : Optional.empty();
    }

    @Override
    public Optional<Artwork> findPublishedById(ArtworkId id) {
      return findById(id).filter(value -> value.status() == ArtworkStatus.PUBLISHED);
    }

    @Override
    public Optional<Artwork> findByOwnerId(UserId ownerId, ArtworkId id) {
      return findById(id).filter(value -> value.ownerId().equals(ownerId));
    }

    @Override
    public List<Artwork> findPublishedPage(PublishedPageQuery query) {
      return List.of();
    }

    @Override
    public List<Artwork> findOwnedPage(OwnedPageQuery query) {
      return List.of();
    }

    @Override
    public Artwork save(Artwork value) {
      artwork = value;
      return value;
    }
  }
}
