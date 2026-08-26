package com.creatorhub.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.catalog.application.port.out.MediaObjectStore;
import com.creatorhub.catalog.application.port.out.MediaStoragePort;
import com.creatorhub.catalog.domain.model.MediaUploadRequest;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证媒体上传必须先登记归属，并且只有存储中真实存在的对象才能转为 READY。 */
class MediaUploadServiceTest {

  private static final Instant EXPIRES_AT = Instant.parse("2026-08-26T01:00:00Z");

  @Test
  void createsPendingObjectAndReturnsPresignedTarget() {
    RecordingObjects objects = new RecordingObjects();
    RecordingStorage storage = new RecordingStorage();
    MediaUploadService service = new MediaUploadService(objects, storage, 900);
    UUID ownerId = UUID.randomUUID();

    var result =
        service.createUpload(
            ownerId,
            new MediaUploadRequest(
                "cover.webp", "image/webp", 1024, MediaUploadRequest.Purpose.ARTWORK));

    assertThat(result.mediaId()).isEqualTo(objects.mediaId);
    assertThat(result.method()).isEqualTo("PUT");
    assertThat(objects.ownerId).isEqualTo(ownerId);
    assertThat(objects.objectKey).isEqualTo(ownerId + "/" + result.mediaId());
    assertThat(storage.objectKey).isEqualTo(objects.objectKey);
  }

  @Test
  void confirmsOnlyWhenStorageContainsPendingObject() {
    RecordingObjects objects = new RecordingObjects();
    RecordingStorage storage = new RecordingStorage();
    storage.exists = true;
    MediaUploadService service = new MediaUploadService(objects, storage, 900);
    UUID ownerId = UUID.randomUUID();
    UUID mediaId = UUID.randomUUID();
    objects.pending.put(mediaId, ownerId + "/" + mediaId);

    service.confirmUpload(ownerId, mediaId);

    assertThat(objects.markReadyCalled).isTrue();
  }

  @Test
  void missingStorageObjectCannotBecomeReady() {
    RecordingObjects objects = new RecordingObjects();
    RecordingStorage storage = new RecordingStorage();
    MediaUploadService service = new MediaUploadService(objects, storage, 900);
    UUID ownerId = UUID.randomUUID();
    UUID mediaId = UUID.randomUUID();
    objects.pending.put(mediaId, ownerId + "/" + mediaId);

    assertThatThrownBy(() -> service.confirmUpload(ownerId, mediaId))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("MEDIA_OBJECT_NOT_FOUND");
    assertThat(objects.markReadyCalled).isFalse();
  }

  private static final class RecordingObjects implements MediaObjectStore {
    private final Map<UUID, String> pending = new HashMap<>();
    private UUID mediaId;
    private UUID ownerId;
    private String objectKey;
    private boolean markReadyCalled;

    @Override
    public void createPending(
        UUID mediaId,
        UUID ownerId,
        String objectKey,
        String contentType,
        long sizeBytes,
        Instant expiresAt,
        Instant createdAt) {
      this.mediaId = mediaId;
      this.ownerId = ownerId;
      this.objectKey = objectKey;
      pending.put(mediaId, objectKey);
    }

    @Override
    public String objectKeyForReadyCandidate(UUID mediaId, UUID ownerId) {
      String objectKey = pending.get(mediaId);
      if (objectKey == null || !objectKey.startsWith(ownerId + "/")) {
        throw new BusinessError("MEDIA_NOT_READY_OR_NOT_OWNED", "Media upload cannot be confirmed");
      }
      return objectKey;
    }

    @Override
    public boolean markReady(UUID mediaId, UUID ownerId, Instant uploadedAt) {
      markReadyCalled = true;
      return true;
    }

    @Override
    public Optional<PublicMedia> findPublished(UUID mediaId) {
      return Optional.empty();
    }
  }

  private static final class RecordingStorage implements MediaStoragePort {
    private boolean exists;
    private String objectKey;

    @Override
    public PresignedUpload presign(
        UUID mediaId, String objectKey, MediaUploadRequest request, Instant expiresAt) {
      this.objectKey = objectKey;
      return new PresignedUpload(
          "http://localhost/upload", "PUT", request.contentType(), expiresAt);
    }

    @Override
    public boolean exists(String objectKey) {
      return exists;
    }

    @Override
    public String presignGet(String objectKey, String contentType, Instant expiresAt) {
      throw new UnsupportedOperationException("Public media reads are not used in upload tests");
    }
  }
}
