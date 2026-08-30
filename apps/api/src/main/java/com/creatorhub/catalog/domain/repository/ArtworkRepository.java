package com.creatorhub.catalog.domain.repository;

import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.catalog.domain.model.ProductType;
import com.creatorhub.sharedkernel.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 以 Artwork 聚合为单位定义 Catalog 仓储端口，禁止上层依赖 MyBatis Row 或 Mapper。 */
public interface ArtworkRepository {

  Optional<Artwork> findById(ArtworkId id);

  Optional<Artwork> findPublishedById(ArtworkId id);

  Optional<Artwork> findByOwnerId(UserId ownerId, ArtworkId id);

  List<Artwork> findPublishedPage(PublishedPageQuery query);

  /** 按作品 owner 查询 Studio 列表，状态筛选和游标由数据库执行。 */
  List<Artwork> findOwnedPage(OwnedPageQuery query);

  record PublishedPageQuery(
      String query,
      String category,
      ProductType productType,
      UUID creatorId,
      Instant beforeCreatedAt,
      ArtworkId beforeId,
      int limit) {}

  record OwnedPageQuery(
      UserId ownerId,
      com.creatorhub.catalog.domain.model.ArtworkStatus status,
      Instant beforeUpdatedAt,
      ArtworkId beforeId,
      int limit) {}

  Artwork save(Artwork artwork);
}
