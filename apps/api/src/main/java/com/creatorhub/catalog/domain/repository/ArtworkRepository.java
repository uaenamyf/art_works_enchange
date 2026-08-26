package com.creatorhub.catalog.domain.repository;

import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.sharedkernel.UserId;
import java.util.Optional;

/** 以 Artwork 聚合为单位定义 Catalog 仓储端口，禁止上层依赖 MyBatis Row 或 Mapper。 */
public interface ArtworkRepository {

  Optional<Artwork> findById(ArtworkId id);

  Optional<Artwork> findByOwnerId(UserId ownerId, ArtworkId id);

  Artwork save(Artwork artwork);
}
