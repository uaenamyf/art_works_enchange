package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.catalog.domain.repository.ArtworkRepository;
import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.sharedkernel.error.BusinessError;
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

  public ArtworkService(ArtworkRepository artworks, CreatorUseCases creators) {
    this.artworks = artworks;
    this.creators = creators;
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
            AppClock.now());
    return view(artworks.save(artwork));
  }

  @Override
  @Transactional(readOnly = true)
  public ArtworkView getPublished(UUID artworkId) {
    return artworks
        .findPublishedById(ArtworkId.of(artworkId))
        .map(ArtworkService::view)
        .orElseThrow(() -> new BusinessError("ARTWORK_NOT_FOUND", "Artwork not found"));
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
        artwork.version());
  }
}
