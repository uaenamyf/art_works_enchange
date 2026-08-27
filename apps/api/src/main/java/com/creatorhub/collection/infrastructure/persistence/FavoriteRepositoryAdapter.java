package com.creatorhub.collection.infrastructure.persistence;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.collection.application.port.in.FavoriteUseCases;
import com.creatorhub.collection.domain.repository.FavoriteRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 收藏仓储适配器；只传递 UserId/ArtworkId，不依赖 Catalog 持久化对象。 */
@Repository
public class FavoriteRepositoryAdapter implements FavoriteRepository, FavoriteUseCases.ReadPort {

  private final FavoriteMapper mapper;

  public FavoriteRepositoryAdapter(FavoriteMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean add(UserId userId, ArtworkId artworkId) {
    return mapper.add(userId.value(), artworkId.value(), AppClock.now()) == 1;
  }

  @Override
  public boolean remove(UserId userId, ArtworkId artworkId) {
    return mapper.remove(userId.value(), artworkId.value()) == 1;
  }

  @Override
  public long countByArtworkId(ArtworkId artworkId) {
    return mapper.countByArtworkId(artworkId.value());
  }

  @Override
  public long countByArtworkId(UUID artworkId) {
    return countByArtworkId(ArtworkId.of(artworkId));
  }

  @Override
  public boolean isFavorited(UUID userId, UUID artworkId) {
    return mapper.exists(userId, artworkId) > 0;
  }

  @Override
  public List<FavoriteEntry> findPage(
      UserId userId, java.time.Instant beforeCreatedAt, ArtworkId beforeArtworkId, int limit) {
    return mapper
        .findPage(
            userId.value(),
            beforeCreatedAt,
            beforeArtworkId == null ? null : beforeArtworkId.value(),
            limit)
        .stream()
        .map(row -> new FavoriteEntry(ArtworkId.of(row.getArtworkId()), row.getCreatedAt()))
        .toList();
  }
}
