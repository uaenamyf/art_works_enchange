package com.creatorhub.collection.interfaces.rest;

import com.creatorhub.collection.application.port.in.FavoriteUseCases;
import com.creatorhub.collection.application.port.in.FavoriteUseCases.FavoriteView;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 收藏 HTTP 适配器；用户身份只从认证 principal 获取，不能由请求体伪造。 */
@RestController
@RequestMapping("/api/v1")
public class FavoriteController {

  private final FavoriteUseCases favorites;

  public FavoriteController(FavoriteUseCases favorites) {
    this.favorites = favorites;
  }

  /** 返回当前用户的收藏作品，详情由 Collection 通过 Catalog 公开端口组装。 */
  @org.springframework.web.bind.annotation.GetMapping("/me/favorites")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<FavoriteUseCases.FavoritePage> list(
      @AuthenticationPrincipal UUID userId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(favorites.list(userId, cursor, limit));
  }

  /** 收藏已发布作品；重复 PUT 返回当前状态和最新计数。 */
  @PutMapping("/favorites/{artworkId}")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<FavoriteView> favorite(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    return ResponseEntity.ok(favorites.favorite(userId, artworkId));
  }

  /** 取消收藏；重复 DELETE 保持幂等并返回 204。 */
  @DeleteMapping("/favorites/{artworkId}")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<Void> unfavorite(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    favorites.unfavorite(userId, artworkId);
    return ResponseEntity.noContent().build();
  }
}
