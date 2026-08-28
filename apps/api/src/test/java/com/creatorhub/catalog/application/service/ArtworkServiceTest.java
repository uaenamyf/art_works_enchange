package com.creatorhub.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import com.creatorhub.catalog.domain.repository.ArtworkRepository;
import com.creatorhub.collection.application.port.in.FavoriteUseCases;
import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 验证公开列表使用稳定游标，并在应用层补齐 hasMore 与下一页游标。 */
class ArtworkServiceTest {

  private static final Instant CREATED_AT = Instant.parse("2026-08-26T00:00:00Z");

  @Test
  void fetchesOneExtraRecordAndForwardsFilters() {
    ArtworkRepository repository = mock(ArtworkRepository.class);
    CreatorUseCases creators = mock(CreatorUseCases.class);
    ArtworkService service = new ArtworkService(repository, creators, favoriteStats());
    Artwork first = publishedArtwork("first");
    Artwork second = publishedArtwork("second");
    UUID creatorId = UUID.randomUUID();
    when(repository.findPublishedPage(any())).thenReturn(List.of(first, second));

    ArtworkUseCases.ArtworkPage page =
        service.listPublished("  vase ", "ART", creatorId, "LATEST", null, 1);

    ArgumentCaptor<ArtworkRepository.PublishedPageQuery> query =
        ArgumentCaptor.forClass(ArtworkRepository.PublishedPageQuery.class);
    verify(repository).findPublishedPage(query.capture());
    assertThat(query.getValue().query()).isEqualTo("vase");
    assertThat(query.getValue().category()).isEqualTo("ART");
    assertThat(query.getValue().creatorId()).isEqualTo(creatorId);
    assertThat(query.getValue().limit()).isEqualTo(2);
    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).coverMediaId()).isEqualTo(first.mediaIds().get(0));
    assertThat(page.hasMore()).isTrue();
    assertThat(page.nextCursor()).isNotBlank();
  }

  @Test
  void publishesArtworkPublishedEventForOwnerWithoutSensitiveFields() {
    ArtworkRepository repository = mock(ArtworkRepository.class);
    CreatorUseCases creators = mock(CreatorUseCases.class);
    OutboxRepository outbox = mock(OutboxRepository.class);
    ArtworkService service = new ArtworkService(repository, creators, favoriteStats(), outbox);
    UUID ownerId = UUID.randomUUID();
    Artwork artwork = approvedArtwork(ownerId, "new work");
    when(repository.findByOwnerId(UserId.of(ownerId), artwork.id()))
        .thenReturn(java.util.Optional.of(artwork));
    when(repository.save(artwork)).thenReturn(artwork);

    service.publish(ownerId, artwork.id().value());

    ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outbox).append(event.capture());
    assertThat(event.getValue().eventType()).isEqualTo("ArtworkPublished");
    assertThat(event.getValue().payload().get("recipientId")).isEqualTo(ownerId.toString());
    assertThat(event.getValue().payload().get("artworkId"))
        .isEqualTo(artwork.id().value().toString());
    assertThat(event.getValue().payload()).doesNotContainKeys("price", "stock", "mediaObjectKey");
  }

  private static FavoriteUseCases.ReadPort favoriteStats() {
    return new FavoriteUseCases.ReadPort() {
      public long countByArtworkId(UUID artworkId) {
        return 0;
      }

      public boolean isFavorited(UUID userId, UUID artworkId) {
        return false;
      }
    };
  }

  private static Artwork publishedArtwork(String title) {
    Artwork artwork = approvedArtwork(title);
    artwork.publish(CREATED_AT);
    assertThat(artwork.status()).isEqualTo(ArtworkStatus.PUBLISHED);
    return artwork;
  }

  private static Artwork approvedArtwork(String title) {
    return approvedArtwork(UUID.randomUUID(), title);
  }

  private static Artwork approvedArtwork(UUID ownerId, String title) {
    Artwork artwork =
        Artwork.draft(
            ArtworkId.random(),
            UserId.of(ownerId),
            UUID.randomUUID(),
            title,
            "A sufficiently long description for an artwork.",
            "ART",
            Money.of("99.00", "USD"),
            2,
            List.of(UUID.randomUUID()),
            CREATED_AT);
    artwork.submitForReview(CREATED_AT);
    artwork.approve(CREATED_AT);
    assertThat(artwork.status()).isEqualTo(ArtworkStatus.APPROVED);
    return artwork;
  }
}
