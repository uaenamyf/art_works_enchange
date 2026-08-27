package com.creatorhub.collection.application.port.in;

import java.util.UUID;

/** 收藏公开用例；调用方身份来自认证上下文，作品 ID 只作为跨上下文引用。 */
public interface FavoriteUseCases {

  FavoriteView favorite(UUID userId, UUID artworkId);

  FavoriteView unfavorite(UUID userId, UUID artworkId);

  record FavoriteView(boolean favorited, long favoriteCount) {}
}
