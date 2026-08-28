package com.creatorhub.catalog.interfaces.rest;

import com.creatorhub.catalog.application.port.in.DigitalAssetUploadUseCases;
import com.creatorhub.catalog.application.port.in.DigitalAssetUploadUseCases.UploadView;
import com.creatorhub.catalog.domain.model.DigitalAssetUploadRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

/** 数字资产上传 HTTP 适配器；原始文件只进入私有存储，确认后进入扫描状态。 */
@RestController
@RequestMapping("/api/v1/digital-assets")
public class DigitalAssetController {

  private final DigitalAssetUploadUseCases uploads;

  public DigitalAssetController(DigitalAssetUploadUseCases uploads) {
    this.uploads = uploads;
  }

  @PostMapping("/upload-requests")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<UploadView> createUpload(
      @AuthenticationPrincipal UUID ownerId,
      @Valid @RequestBody DigitalAssetUploadRequestBody request) {
    DigitalAssetUploadRequest domain = request.toDomain();
    return ResponseEntity.status(201)
        .body(uploads.createUpload(ownerId, request.artworkId(), domain));
  }

  @PostMapping("/{versionId}/confirm")
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<Void> confirmUpload(
      @AuthenticationPrincipal UUID ownerId, @PathVariable UUID versionId) {
    uploads.confirmUpload(ownerId, versionId);
    return ResponseEntity.noContent().build();
  }

  /** 数字文件只接受私有资产所需的元数据和预先计算的 SHA-256。 */
  record DigitalAssetUploadRequestBody(
      UUID artworkId,
      @NotBlank @Size(max = 255) String fileName,
      @NotBlank String contentType,
      @Min(1) @Max(DigitalAssetUploadRequest.MAX_SIZE_BYTES) long sizeBytes,
      @NotBlank @Size(min = 64, max = 64) String sha256) {

    DigitalAssetUploadRequest toDomain() {
      return new DigitalAssetUploadRequest(fileName, contentType, sizeBytes, sha256);
    }
  }
}
