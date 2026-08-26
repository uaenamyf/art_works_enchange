package com.creatorhub.catalog.interfaces.rest;

import com.creatorhub.catalog.application.port.in.MediaUploadUseCases;
import com.creatorhub.catalog.application.port.in.MediaUploadUseCases.UploadView;
import com.creatorhub.catalog.domain.model.MediaUploadRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 媒体上传 HTTP 适配器；只签发短期 URL，上传完成必须由服务端确认对象存在。 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

  private final MediaUploadUseCases uploads;

  public MediaController(MediaUploadUseCases uploads) {
    this.uploads = uploads;
  }

  /** 创建媒体上传请求，记录归属后才返回预签名 PUT URL。 */
  @PostMapping("/upload-requests")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<UploadView> createUpload(
      @AuthenticationPrincipal UUID ownerId, @Valid @RequestBody UploadRequest request) {
    return ResponseEntity.status(201).body(uploads.createUpload(ownerId, request.toDomain()));
  }

  /** 确认上传结果；服务端会检查对象存储中的真实对象后才标记 READY。 */
  @PostMapping("/{mediaId}/confirm")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<Void> confirmUpload(
      @AuthenticationPrincipal UUID ownerId, @PathVariable UUID mediaId) {
    uploads.confirmUpload(ownerId, mediaId);
    return ResponseEntity.noContent().build();
  }

  /** 与 OpenAPI UploadRequest 对齐的 HTTP 输入模型。 */
  record UploadRequest(
      @NotBlank @Size(max = 255) String fileName,
      @NotBlank String contentType,
      @Min(1) @Max(MediaUploadRequest.MAX_SIZE_BYTES) long sizeBytes,
      @NotNull MediaUploadRequest.Purpose purpose) {

    MediaUploadRequest toDomain() {
      return new MediaUploadRequest(fileName, contentType, sizeBytes, purpose);
    }
  }
}
