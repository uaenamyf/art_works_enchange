package com.creatorhub.collection.application.service;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.collection.application.port.in.FavoriteUseCases;
import com.creatorhub.collection.domain.repository.FavoriteRepository;
import com.creatorhub.sharedkernel.UserId;
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
}
