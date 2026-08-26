package com.creatorhub.catalog.application.port.in;

import com.creatorhub.catalog.domain.model.MediaUploadRequest;
import java.time.Instant;
import java.util.UUID;

/** 媒体上传用例端口，上传完成前的对象只能保持 PENDING，不得绑定到作品。 */
public interface MediaUploadUseCases {

  UploadView createUpload(UUID ownerId, MediaUploadRequest request);

  void confirmUpload(UUID ownerId, UUID mediaId);

  record UploadView(
      UUID mediaId, String uploadUrl, String method, String contentType, Instant expiresAt) {}
}
