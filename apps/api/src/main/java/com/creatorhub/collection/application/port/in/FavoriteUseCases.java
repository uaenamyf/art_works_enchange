package com.creatorhub.collection.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 收藏公开用例；调用方身份来自认证上下文，作品 ID 只作为跨上下文引用。 */
public interface FavoriteUseCases {

  FavoriteView favorite(UUID userId, UUID artworkId);

  FavoriteView unfavorite(UUID userId, UUID artworkId);

  interface ReadPort {
    long countByArtworkId(UUID artworkId);

    boolean isFavorited(UUID userId, UUID artworkId);
  }

  FavoritePage list(UUID userId, String cursor, int limit);

  record FavoriteView(boolean favorited, long favoriteCount) {}

  record FavoritePage(List<FavoriteArtworkView> items, String nextCursor, boolean hasMore) {}

  record FavoriteArtworkView(
      UUID id,
      UUID creatorId,
      String title,
      String description,
      String category,
      java.math.BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      List<UUID> mediaIds,
      Instant favoritedAt) {}
}
