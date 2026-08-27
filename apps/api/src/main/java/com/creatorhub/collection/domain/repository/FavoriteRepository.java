package com.creatorhub.collection.domain.repository;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.sharedkernel.UserId;

/** 以用户和作品关系为单位定义收藏仓储端口，数据库唯一键保证同一收藏不重复。 */
public interface FavoriteRepository {

  boolean add(UserId userId, ArtworkId artworkId);

  boolean remove(UserId userId, ArtworkId artworkId);

  long countByArtworkId(ArtworkId artworkId);
}
