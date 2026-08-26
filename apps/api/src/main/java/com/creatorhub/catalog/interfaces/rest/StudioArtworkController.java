package com.creatorhub.catalog.interfaces.rest;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Studio 作品查询适配器；只返回当前 Creator 自己的作品，避免草稿状态进入公开接口。 */
@RestController
@RequestMapping("/api/v1/studio/artworks")
public class StudioArtworkController {

  private final ArtworkUseCases artworks;

  public StudioArtworkController(ArtworkUseCases artworks) {
    this.artworks = artworks;
  }

  /** 读取编辑视图；对象级归属校验由应用服务通过认证用户 ID 完成。 */
  @GetMapping("/{artworkId}")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkView> getOwned(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    return ResponseEntity.ok(artworks.getOwned(userId, artworkId));
  }
}