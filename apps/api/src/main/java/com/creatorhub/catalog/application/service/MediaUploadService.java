package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.in.MediaUploadUseCases;
import com.creatorhub.catalog.application.port.out.MediaObjectStore;
import com.creatorhub.catalog.application.port.out.MediaStoragePort;
import com.creatorhub.catalog.domain.model.MediaUploadRequest;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 媒体上传应用服务，创建待上传对象并在存储端确认真实对象后转为 READY。 */
@Service
@Transactional
public class MediaUploadService implements MediaUploadUseCases {

  private final MediaObjectStore objects;
  private final MediaStoragePort storage;
  private final long uploadTtlSeconds;

  public MediaUploadService(
      MediaObjectStore objects,
      MediaStoragePort storage,
      @Value("${creatorhub.storage.upload-url-ttl-seconds:900}") long uploadTtlSeconds) {
    this.objects = objects;
    this.storage = storage;
    this.uploadTtlSeconds = uploadTtlSeconds;
  }

  @Override
  public UploadView createUpload(UUID ownerId, MediaUploadRequest request) {
    UUID mediaId = UUID.randomUUID();
    Instant now = AppClock.now();
    Instant expiresAt = now.plus(Duration.ofSeconds(uploadTtlSeconds));
    String objectKey = ownerId + "/" + mediaId;
    objects.createPending(
        mediaId, ownerId, objectKey, request.contentType(), request.sizeBytes(), expiresAt, now);
    MediaStoragePort.PresignedUpload upload =
        storage.presign(mediaId, objectKey, request, expiresAt);
    return new UploadView(
        mediaId, upload.uploadUrl(), upload.method(), upload.contentType(), upload.expiresAt());
  }

  @Override
  public void confirmUpload(UUID ownerId, UUID mediaId) {
    String objectKey = objects.objectKeyForReadyCandidate(mediaId, ownerId);
    if (!storage.exists(objectKey)) {
      throw new BusinessError("MEDIA_OBJECT_NOT_FOUND", "Uploaded media object was not found");
    }
    if (!objects.markReady(mediaId, ownerId, AppClock.now())) {
      throw new BusinessError("MEDIA_NOT_READY_OR_NOT_OWNED", "Media upload cannot be confirmed");
    }
  }
}
