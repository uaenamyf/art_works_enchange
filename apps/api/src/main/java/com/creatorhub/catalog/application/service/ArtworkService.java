package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.catalog.domain.repository.ArtworkRepository;
import com.creatorhub.collection.application.port.in.FavoriteUseCases;
import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Artwork 应用服务，编排 owner 校验、Creator 归属和聚合状态变更。 */
@Service
@Transactional
public class ArtworkService implements ArtworkUseCases {

  private final ArtworkRepository artworks;
  private final CreatorUseCases creators;
  private final FavoriteUseCases.ReadPort favoriteStats;

  public ArtworkService(
      ArtworkRepository artworks,
      CreatorUseCases creators,
      FavoriteUseCases.ReadPort favoriteStats) {
    this.artworks = artworks;
    this.creators = creators;
    this.favoriteStats = favoriteStats;
  }

  @Override
  public ArtworkView create(UUID userId, WriteCommand command) {
    CreatorUseCases.CreatorView creator = creators.getByUserId(userId);
    Artwork artwork =
        Artwork.draft(
            ArtworkId.random(),
            UserId.of(userId),
            creator.id(),
            command.title(),
            command.description(),
            command.category(),
            money(command),
            command.stock(),
            command.mediaIds(),
            AppClock.now(),
            command.productType());
    return view(artworks.save(artwork));
  }

  @Override
  @Transactional(readOnly = true)
  public ArtworkView getPublished(UUID artworkId) {
    return getPublished(artworkId, null);
  }

  @Override
  @Transactional(readOnly = true)
  public ArtworkView getPublished(UUID artworkId, UUID userId) {
    return artworks
        .findPublishedById(ArtworkId.of(artworkId))
        .map(artwork -> view(artwork, userId))
        .orElseThrow(() -> new BusinessError("ARTWORK_NOT_FOUND", "Artwork not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public ArtworkView getOwned(UUID userId, UUID artworkId) {
    return view(owned(userId, artworkId));
  }

  @Override
  public ArtworkView update(UUID userId, UUID artworkId, WriteCommand command) {
    Artwork artwork = owned(userId, artworkId);
    if (artwork.version() != command.version()) {
      throw new BusinessError("ARTWORK_VERSION_CONFLICT", "Artwork version is stale");
    }
    artwork.update(
        command.title(),
        command.description(),
        command.category(),
        money(command),
        command.stock(),
        command.mediaIds(),
        AppClock.now());
    return view(artworks.save(artwork));
  }

  @Override
  public ArtworkView submitForReview(UUID userId, UUID artworkId) {
    Artwork artwork = owned(userId, artworkId);
    artwork.submitForReview(AppClock.now());
    return view(artworks.save(artwork));
  }

  @Override
  public ArtworkView publish(UUID userId, UUID artworkId) {
    Artwork artwork = owned(userId, artworkId);
    artwork.publish(AppClock.now());
    return view(artworks.save(artwork));
  }

  @Override
  public ArtworkView archive(UUID userId, UUID artworkId) {
    Artwork artwork = owned(userId, artworkId);
    artwork.archive(AppClock.now());
    return view(artworks.save(artwork));
  }

  @Override
  @Transactional(readOnly = true)
  public ArtworkPage listPublished(
      String query, String category, UUID creatorId, String sort, String cursor, int limit) {
    return listPublished(query, category, creatorId, sort, cursor, limit, null);
  }

  @Override
  @Transactional(readOnly = true)
  public ArtworkPage listPublished(
      String query,
      String category,
      UUID creatorId,
      String sort,
      String cursor,
      int limit,
      UUID userId) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decode(cursor);
    List<Artwork> results =
        artworks.findPublishedPage(
            new ArtworkRepository.PublishedPageQuery(
                blankToNull(query),
                blankToNull(category),
                creatorId,
                decoded.createdAt(),
                decoded.id(),
                pageSize + 1));
    boolean hasMore = results.size() > pageSize;
    List<Artwork> page = results.stream().limit(pageSize).toList();
    String nextCursor = hasMore ? Cursor.encode(page.get(page.size() - 1)) : null;
    return new ArtworkPage(
        page.stream()
            .map(
                artwork ->
                    new ArtworkListView(
                        artwork.id().value(),
                        artwork.title(),
                        artwork.category(),
                        artwork.price().amount(),
                        artwork.price().currency().getCurrencyCode(),
                        artwork.stock(),
                        artwork.creatorId(),
                        artwork.mediaIds().get(0),
                        artwork.status(),
                        artwork.productType(),
                        artwork.createdAt(),
                        favoriteStats.countByArtworkId(artwork.id().value()),
                        userId != null && favoriteStats.isFavorited(userId, artwork.id().value())))
            .toList(),
        nextCursor,
        hasMore);
  }

  private Artwork owned(UUID userId, UUID artworkId) {
    return artworks
        .findByOwnerId(UserId.of(userId), ArtworkId.of(artworkId))
        .orElseThrow(() -> new BusinessError("ARTWORK_NOT_FOUND", "Artwork not found"));
  }

  private static Money money(WriteCommand command) {
    return Money.of(
        Objects.requireNonNull(command.priceAmount(), "priceAmount").toPlainString(),
        Objects.requireNonNull(command.priceCurrency(), "priceCurrency"));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record Cursor(Instant createdAt, ArtworkId id) {
    static Cursor decode(String value) {
      if (value == null || value.isBlank()) return new Cursor(null, null);
      try {
        String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", 2);
        return new Cursor(Instant.parse(parts[0]), ArtworkId.of(UUID.fromString(parts[1])));
      } catch (RuntimeException ex) {
        throw new BusinessError("INVALID_CURSOR", "Cursor is invalid");
      }
    }

    static String encode(Artwork artwork) {
      String value = artwork.createdAt() + "|" + artwork.id().value();
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
  }

  private ArtworkView view(Artwork artwork) {
    return view(artwork, null);
  }

  private ArtworkView view(Artwork artwork, UUID userId) {
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
        favoriteStats.countByArtworkId(artwork.id().value()),
        userId != null && favoriteStats.isFavorited(userId, artwork.id().value()));
  }
}
