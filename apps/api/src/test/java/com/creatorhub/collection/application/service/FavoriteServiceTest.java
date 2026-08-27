package com.creatorhub.collection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import com.creatorhub.collection.application.port.in.FavoriteUseCases.FavoriteView;
import com.creatorhub.collection.domain.repository.FavoriteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证收藏前必须确认作品公开可见，并返回最新收藏计数。 */
class FavoriteServiceTest {

  @Test
  void favoritesPublishedArtworkIdempotently() {
    FavoriteRepository favorites = mock(FavoriteRepository.class);
    ArtworkUseCases artworks = mock(ArtworkUseCases.class);
    FavoriteService service = new FavoriteService(favorites, artworks);
    UUID userId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(favorites.countByArtworkId(anyArtworkId())).thenReturn(2L);

    FavoriteView result = service.favorite(userId, artworkId);

    assertThat(result).isEqualTo(new FavoriteView(true, 2L));
    verify(artworks).getPublished(artworkId);
    verify(favorites).add(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void unfavoriteRemainsSuccessfulWhenRelationDoesNotExist() {
    FavoriteRepository favorites = mock(FavoriteRepository.class);
    ArtworkUseCases artworks = mock(ArtworkUseCases.class);
    FavoriteService service = new FavoriteService(favorites, artworks);
    UUID userId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(favorites.countByArtworkId(anyArtworkId())).thenReturn(0L);

    FavoriteView result = service.unfavorite(userId, artworkId);

    assertThat(result).isEqualTo(new FavoriteView(false, 0L));
    verify(favorites)
        .remove(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void listsFavoriteArtworkThroughCatalogPort() {
    FavoriteRepository favorites = mock(FavoriteRepository.class);
    ArtworkUseCases artworks = mock(ArtworkUseCases.class);
    FavoriteService service = new FavoriteService(favorites, artworks);
    UUID userId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(favorites.findPage(
            anyUserId(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq(21)))
        .thenReturn(
            List.of(
                new FavoriteRepository.FavoriteEntry(
                    com.creatorhub.catalog.domain.model.ArtworkId.of(artworkId), Instant.now())));
    when(artworks.getPublished(artworkId))
        .thenReturn(
            new ArtworkView(
                artworkId,
                UUID.randomUUID(),
                "Artwork",
                "Description",
                "ART",
                java.math.BigDecimal.TEN,
                "USD",
                1,
                List.of(UUID.randomUUID()),
                com.creatorhub.catalog.domain.model.ArtworkStatus.PUBLISHED,
                0,
                0,
                false));

    var result = service.list(userId, null, 20);

    assertThat(result.items()).hasSize(1);
    verify(artworks).getPublished(artworkId);
  }

  private static com.creatorhub.sharedkernel.UserId anyUserId() {
    return org.mockito.ArgumentMatchers.any();
  }

  private static com.creatorhub.catalog.domain.model.ArtworkId anyArtworkId() {
    return org.mockito.ArgumentMatchers.any();
  }
}
