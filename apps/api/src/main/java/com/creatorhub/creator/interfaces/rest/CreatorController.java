package com.creatorhub.creator.interfaces.rest;

import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.creator.application.port.in.CreatorUseCases.ApplyCommand;
import com.creatorhub.creator.application.port.in.CreatorUseCases.CreatorView;
import com.creatorhub.creator.application.port.in.CreatorUseCases.UpdateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

/**
 * Creator HTTP 适配器：公开查询创作者，认证用户申请或更新自己的档案。
 *
 * <p>owner 只来自安全上下文 UserId，客户端请求体不能指定资源所有者或角色。
 */
@RestController
@RequestMapping("/api/v1")
public class CreatorController {

  private final CreatorUseCases creators;

  public CreatorController(CreatorUseCases creators) {
    this.creators = creators;
  }

  /** 公开 Creator 列表；只读查询按创建时间稳定分页，统计字段暂由占位投影返回。 */
  @GetMapping("/creators")
  ResponseEntity<CreatorUseCases.CreatorPage> list(
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(creators.list(category, cursor, limit));
  }

  /** 申请 Creator，成功返回 201 与公开资源 Location。 */
  @PostMapping("/me/creator")
  @PreAuthorize("hasRole('USER')")
  ResponseEntity<CreatorDetailResponse> apply(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody ApplyCreatorRequest request) {
    CreatorView created =
        creators.apply(
            userId,
            new ApplyCommand(
                request.displayName(),
                request.slug(),
                request.category(),
                request.bio(),
                request.location()));
    return ResponseEntity.created(URI.create("/api/v1/creators/" + created.id()))
        .body(CreatorDetailResponse.from(created));
  }

  /** 更新当前认证用户拥有的 Creator 档案，不接受其他 creatorId。 */
  @PutMapping("/me/creator")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<CreatorDetailResponse> update(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateCreatorRequest request) {
    CreatorView updated =
        creators.update(
            userId, new UpdateCommand(request.displayName(), request.bio(), request.location()));
    return ResponseEntity.ok(CreatorDetailResponse.from(updated));
  }

  /** 获取公开 Creator 详情；关注数和作品数将在后续上下文接入后替换零值投影。 */
  @GetMapping("/creators/{creatorId}")
  ResponseEntity<CreatorDetailResponse> get(@PathVariable UUID creatorId) {
    return ResponseEntity.ok(CreatorDetailResponse.from(creators.get(creatorId)));
  }

  /** 与 OpenAPI ApplyCreatorRequest 对齐的输入模型。 */
  record ApplyCreatorRequest(
      @NotBlank @Size(min = 2, max = 50) String displayName,
      @NotBlank @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{1,38}[a-z0-9])?$") String slug,
      @NotBlank String category,
      @NotBlank @Size(max = 2000) String bio,
      @Size(max = 100) String location) {}

  /** 与 OpenAPI UpdateCreatorRequest 对齐；分类与 slug 在当前版本不可修改。 */
  record UpdateCreatorRequest(
      @NotBlank @Size(min = 2, max = 50) String displayName,
      @NotBlank @Size(max = 2000) String bio,
      @Size(max = 100) String location) {}

  /** CreatorDetail 占位投影；Follow/Catalog 接入后替换计数字段。 */
  record CreatorDetailResponse(
      UUID id,
      String slug,
      String displayName,
      String avatarUrl,
      String category,
      boolean verified,
      long followerCount,
      boolean followedByMe,
      String bio,
      String coverUrl,
      String location,
      long artworkCount,
      List<String> socialLinks) {

    static CreatorDetailResponse from(CreatorView view) {
      return new CreatorDetailResponse(
          view.id(),
          view.slug(),
          view.displayName(),
          null,
          view.category(),
          false,
          0,
          false,
          view.bio(),
          null,
          view.location(),
          0,
          List.of());
    }
  }
}
