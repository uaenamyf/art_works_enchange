package com.creatorhub.collection.infrastructure.persistence;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.collection.domain.repository.FavoriteRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import org.springframework.stereotype.Repository;

/** 收藏仓储适配器；只传递 UserId/ArtworkId，不依赖 Catalog 持久化对象。 */
@Repository
public class FavoriteRepositoryAdapter implements FavoriteRepository {

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
}
