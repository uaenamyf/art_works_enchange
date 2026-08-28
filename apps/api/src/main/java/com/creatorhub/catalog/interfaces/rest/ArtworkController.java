package com.creatorhub.catalog.interfaces.rest;

import com.creatorhub.catalog.application.port.in.ArtworkReviewUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.WriteCommand;
import com.creatorhub.catalog.domain.model.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Artwork 写接口适配器；owner 只来自安全上下文，审核状态只能由后台审核用例推进。 */
@RestController
@RequestMapping("/api/v1/artworks")
public class ArtworkController {

  private final ArtworkUseCases artworks;
  private final ArtworkReviewUseCases reviews;

  public ArtworkController(ArtworkUseCases artworks, ArtworkReviewUseCases reviews) {
    this.artworks = artworks;
    this.reviews = reviews;
  }

  /** 公开作品列表；数据库只读取 PUBLISHED，排序参数保留为兼容契约入口。 */
  @GetMapping
  ResponseEntity<ArtworkUseCases.ArtworkPage> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) UUID creatorId,
      @RequestParam(defaultValue = "LATEST") String sort,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit,
      @AuthenticationPrincipal UUID userId) {
    return ResponseEntity.ok(
        artworks.listPublished(query, category, creatorId, sort, cursor, limit, userId));
  }

  /** 公开详情只读取审核通过并已发布的作品，未发布状态统一按不存在处理。 */
  @GetMapping("/{artworkId}")
  ResponseEntity<ArtworkView> getPublished(
      @PathVariable UUID artworkId, @AuthenticationPrincipal UUID userId) {
    return ResponseEntity.ok(artworks.getPublished(artworkId, userId));
  }

  /** 创建草稿；作品默认 DRAFT，不因创建成功直接公开。 */
  @PostMapping
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkView> create(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody ArtworkWriteRequest request) {
    ArtworkView artwork = artworks.create(userId, request.command());
    return ResponseEntity.created(URI.create("/api/v1/artworks/" + artwork.id())).body(artwork);
  }

  /** 更新自己的草稿或被拒绝作品，聚合会继续阻止已发布作品编辑。 */
  @PutMapping("/{artworkId}")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkView> update(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID artworkId,
      @Valid @RequestBody ArtworkWriteRequest request) {
    return ResponseEntity.ok(artworks.update(userId, artworkId, request.command()));
  }

  /** 提交审核；提交后只能由审核流程决定 APPROVED 或 REJECTED。 */
  @PostMapping("/{artworkId}/submit-review")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkView> submitReview(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    return ResponseEntity.ok(artworks.submitForReview(userId, artworkId));
  }

  /** 发布已审核通过的作品；未通过审核时由聚合拒绝。 */
  @PostMapping("/{artworkId}/publish")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkView> publish(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    return ResponseEntity.ok(artworks.publish(userId, artworkId));
  }

  /** 归档自己的已发布作品。 */
  @PostMapping("/{artworkId}/archive")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<ArtworkView> archive(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID artworkId) {
    return ResponseEntity.ok(artworks.archive(userId, artworkId));
  }

  /** 管理员批准作品；审核决定不能由 Creator 自己调用。 */
  @PostMapping("/{artworkId}/review/approve")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<ArtworkView> approve(
      @AuthenticationPrincipal UUID reviewerId,
      @PathVariable UUID artworkId,
      @Valid @RequestBody ReviewRequest request) {
    return ResponseEntity.ok(reviews.approve(reviewerId, artworkId, request.reason()));
  }

  /** 管理员拒绝作品；理由会写入审核审计记录。 */
  @PostMapping("/{artworkId}/review/reject")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<ArtworkView> reject(
      @AuthenticationPrincipal UUID reviewerId,
      @PathVariable UUID artworkId,
      @Valid @RequestBody ReviewRequest request) {
    return ResponseEntity.ok(reviews.reject(reviewerId, artworkId, request.reason()));
  }

  /** 与 OpenAPI ArtworkWriteRequest 对齐的写入模型。 */
  record ArtworkWriteRequest(
      @NotBlank @Size(min = 2, max = 160) String title,
      @NotBlank @Size(min = 10, max = 10000) String description,
      @NotBlank @Size(max = 50) String category,
      @NotNull @Valid PriceRequest price,
      @PositiveOrZero int stock,
      @NotNull @Size(max = 12) List<UUID> mediaIds,
      @PositiveOrZero long version,
      ProductType productType) {

    ArtworkWriteRequest(
        String title,
        String description,
        String category,
        PriceRequest price,
        int stock,
        List<UUID> mediaIds,
        long version) {
      this(title, description, category, price, stock, mediaIds, version, ProductType.PHYSICAL);
    }

    WriteCommand command() {
      return new WriteCommand(
          title,
          description,
          category,
          price.amount(),
          price.currency(),
          stock,
          mediaIds,
          version,
          productType == null ? ProductType.PHYSICAL : productType);
    }
  }

  /** 与 OpenAPI Money 对齐；金额在应用服务中转换为领域值对象。 */
  record PriceRequest(
      @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
      @NotBlank @Size(min = 3, max = 3) String currency) {}

  /** 审核理由是审计记录必填字段，避免出现无法解释的状态变更。 */
  record ReviewRequest(@NotBlank @Size(max = 2000) String reason) {}
}
