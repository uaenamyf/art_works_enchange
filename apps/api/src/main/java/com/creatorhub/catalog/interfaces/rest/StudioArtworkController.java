package com.creatorhub.catalog.interfaces.rest;

import com.creatorhub.catalog.application.port.in.ArtworkReviewUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Studio 作品查询适配器；只返回当前 Creator 自己的作品，避免草稿状态进入公开接口。 */
@RestController
@RequestMapping("/api/v1/studio/artworks")
public class StudioArtworkController {

  private final ArtworkUseCases artworks;
  private final ArtworkReviewUseCases reviews;

  public StudioArtworkController(ArtworkUseCases artworks, ArtworkReviewUseCases reviews) {
    this.artworks = artworks;
    this.reviews = reviews;
  }

  /** 读取编辑视图；对象级归属校验由应用服务通过认证用户 ID 完成。 */
  @GetMapping("/{artworkId}")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkView> getOwned(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    return ResponseEntity.ok(artworks.getOwned(userId, artworkId));
  }

  /** 返回当前 Creator 的作品列表；草稿和审核状态只在此受保护入口可见。 */
  @GetMapping
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkUseCases.StudioArtworkPage> listOwned(
      @AuthenticationPrincipal UUID userId,
      @RequestParam(required = false) ArtworkStatus status,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(artworks.listOwned(userId, status, cursor, limit));
  }

  /** Creator 只能读取自己作品的审核记录，拒绝理由用于指导作品修改。 */
  @GetMapping("/{artworkId}/reviews")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<List<ArtworkReviewUseCases.ReviewView>> listReviews(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    return ResponseEntity.ok(reviews.listForOwner(userId, artworkId));
  }
}
