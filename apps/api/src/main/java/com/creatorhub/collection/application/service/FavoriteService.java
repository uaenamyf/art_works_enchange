package com.creatorhub.collection.application.service;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.collection.application.port.in.FavoriteUseCases;
import com.creatorhub.collection.domain.repository.FavoriteRepository;
import com.creatorhub.sharedkernel.UserId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 收藏应用服务；先通过 Catalog 公开端口确认作品可见，再执行幂等关系变更。 */
@Service
@Transactional
public class FavoriteService implements FavoriteUseCases {

  private final FavoriteRepository favorites;
  private final ArtworkUseCases artworks;

  public FavoriteService(FavoriteRepository favorites, ArtworkUseCases artworks) {
    this.favorites = favorites;
    this.artworks = artworks;
  }

  @Override
  public FavoriteView favorite(UUID userId, UUID artworkId) {
    ArtworkId id = ArtworkId.of(artworkId);
    artworks.getPublished(artworkId);
    favorites.add(UserId.of(userId), id);
    return new FavoriteView(true, favorites.countByArtworkId(id));
  }

  @Override
  public FavoriteView unfavorite(UUID userId, UUID artworkId) {
    ArtworkId id = ArtworkId.of(artworkId);
    favorites.remove(UserId.of(userId), id);
    return new FavoriteView(false, favorites.countByArtworkId(id));
  }

  @Override
  @Transactional(readOnly = true)
  public FavoritePage list(UUID userId, String cursor, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decode(cursor);
    List<FavoriteRepository.FavoriteEntry> entries =
        favorites.findPage(
            UserId.of(userId), decoded.createdAt(), decoded.artworkId(), pageSize + 1);
    boolean hasMore = entries.size() > pageSize;
    List<FavoriteRepository.FavoriteEntry> page = entries.stream().limit(pageSize).toList();
    List<FavoriteArtworkView> items =
        page.stream()
            .map(
                entry ->
                    toView(artworks.getPublished(entry.artworkId().value()), entry.createdAt()))
            .toList();
    return new FavoritePage(
        items, hasMore ? Cursor.encode(page.get(page.size() - 1)) : null, hasMore);
  }

  private static FavoriteArtworkView toView(ArtworkView artwork, Instant favoritedAt) {
    return new FavoriteArtworkView(
        artwork.id(),
        artwork.creatorId(),
        artwork.title(),
        artwork.description(),
        artwork.category(),
        artwork.priceAmount(),
        artwork.priceCurrency(),
        artwork.stock(),
        artwork.mediaIds(),
        favoritedAt);
  }

  private record Cursor(Instant createdAt, ArtworkId artworkId) {
    static Cursor decode(String value) {
      if (value == null || value.isBlank()) return new Cursor(null, null);
      try {
        String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", 2);
        return new Cursor(Instant.parse(parts[0]), ArtworkId.of(UUID.fromString(parts[1])));
      } catch (RuntimeException ex) {
        throw new com.creatorhub.sharedkernel.error.BusinessError(
            "INVALID_CURSOR", "Cursor is invalid");
      }
    }

    static String encode(FavoriteRepository.FavoriteEntry entry) {
      String value = entry.createdAt() + "|" + entry.artworkId().value();
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
  }
}
