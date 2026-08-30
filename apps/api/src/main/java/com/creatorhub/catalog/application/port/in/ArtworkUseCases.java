package com.creatorhub.catalog.application.port.in;

import com.creatorhub.catalog.domain.model.ArtworkStatus;
import com.creatorhub.catalog.domain.model.ProductType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Catalog 作品写用例端口；接口层不能绕过该端口直接操作 Repository。 */
public interface ArtworkUseCases {

  ArtworkView create(UUID userId, WriteCommand command);

  ArtworkView getPublished(UUID artworkId);

  default ArtworkView getPublished(UUID artworkId, UUID userId) {
    return getPublished(artworkId);
  }

  ArtworkView getOwned(UUID userId, UUID artworkId);

  ArtworkView update(UUID userId, UUID artworkId, WriteCommand command);

  ArtworkView submitForReview(UUID userId, UUID artworkId);

  ArtworkView publish(UUID userId, UUID artworkId);

  ArtworkView archive(UUID userId, UUID artworkId);

  ArtworkPage listPublished(
      String query, String category, UUID creatorId, String sort, String cursor, int limit);

  ArtworkPage listPublished(
      String query,
      String category,
      ProductType productType,
      UUID creatorId,
      String sort,
      String cursor,
      int limit,
      UUID userId);

  /** Studio 作品列表只返回当前 Creator 自己的作品。 */
  StudioArtworkPage listOwned(UUID userId, ArtworkStatus status, String cursor, int limit);

  default ArtworkPage listPublished(
      String query,
      String category,
      UUID creatorId,
      String sort,
      String cursor,
      int limit,
      UUID userId) {
    return listPublished(query, category, creatorId, sort, cursor, limit);
  }

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
      ProductType productType,
      long version,
      long favoriteCount,
      boolean favoritedByMe) {
    public ArtworkView(
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
        long version,
        long favoriteCount,
        boolean favoritedByMe) {
      this(
          id,
          creatorId,
          title,
          description,
          category,
          priceAmount,
          priceCurrency,
          stock,
          mediaIds,
          status,
          ProductType.PHYSICAL,
          version,
          favoriteCount,
          favoritedByMe);
    }
  }

  record WriteCommand(
      String title,
      String description,
      String category,
      BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      List<UUID> mediaIds,
      long version,
      ProductType productType) {
    public WriteCommand(
        String title,
        String description,
        String category,
        BigDecimal priceAmount,
        String priceCurrency,
        int stock,
        List<UUID> mediaIds,
        long version) {
      this(
          title,
          description,
          category,
          priceAmount,
          priceCurrency,
          stock,
          mediaIds,
          version,
          ProductType.PHYSICAL);
    }
  }

  record ArtworkPage(List<ArtworkListView> items, String nextCursor, boolean hasMore) {}

  record StudioArtworkPage(List<StudioArtworkListView> items, String nextCursor, boolean hasMore) {}

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
      ProductType productType,
      Instant publishedAt,
      long favoriteCount,
      boolean favoritedByMe) {}

  record StudioArtworkListView(
      UUID id,
      String title,
      String category,
      BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      UUID creatorId,
      UUID coverMediaId,
      ArtworkStatus status,
      ProductType productType,
      Instant updatedAt,
      long favoriteCount,
      boolean favoritedByMe) {}
}
