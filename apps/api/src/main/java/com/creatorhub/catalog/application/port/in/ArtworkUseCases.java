package com.creatorhub.catalog.application.port.in;

import com.creatorhub.catalog.domain.model.ArtworkStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Catalog 作品写用例端口；接口层不能绕过该端口直接操作 Repository。 */
public interface ArtworkUseCases {

  ArtworkView create(UUID userId, WriteCommand command);

  ArtworkView getPublished(UUID artworkId);

  ArtworkView getOwned(UUID userId, UUID artworkId);

  ArtworkView update(UUID userId, UUID artworkId, WriteCommand command);

  ArtworkView submitForReview(UUID userId, UUID artworkId);

  ArtworkView publish(UUID userId, UUID artworkId);

  ArtworkView archive(UUID userId, UUID artworkId);

  ArtworkPage listPublished(
      String query, String category, UUID creatorId, String sort, String cursor, int limit);

  /** 只读作品快照，后续可扩展媒体详情而不泄露领域对象。 */
  record ArtworkView(
      UUID id,
      UUID creatorId,
      String title,
      String description,
      String category,
      BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      List<UUID> mediaIds,
      ArtworkStatus status,
      long version) {}

  record WriteCommand(
      String title,
      String description,
      String category,
      BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      List<UUID> mediaIds,
      long version) {}

  record ArtworkPage(List<ArtworkListView> items, String nextCursor, boolean hasMore) {}

  record ArtworkListView(
      UUID id,
      String title,
      String category,
      BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      UUID creatorId,
      UUID coverMediaId,
      ArtworkStatus status,
      Instant publishedAt) {}
}
