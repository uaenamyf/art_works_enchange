package com.creatorhub.catalog.infrastructure.persistence;

import com.creatorhub.catalog.application.port.out.MediaObjectStore;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 媒体对象状态持久化适配器，拒绝跨用户读取和重复确认。 */
@Repository
public class MediaObjectStoreAdapter implements MediaObjectStore {

  private final MediaObjectMapper mapper;

  public MediaObjectStoreAdapter(MediaObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void createPending(
      UUID mediaId,
      UUID ownerId,
      String objectKey,
      String contentType,
      long sizeBytes,
      Instant expiresAt,
      Instant createdAt) {
    MediaObjectPersistenceRow row = new MediaObjectPersistenceRow();
    row.setId(mediaId);
    row.setOwnerId(ownerId);
    row.setObjectKey(objectKey);
    row.setContentType(contentType);
    row.setSizeBytes(sizeBytes);
    row.setUploadStatus("PENDING");
    row.setExpiresAt(expiresAt);
    row.setCreatedAt(createdAt);
    if (mapper.insert(row) != 1) {
      throw new IllegalStateException("Media object was not persisted");
    }
  }

  @Override
  public String objectKeyForReadyCandidate(UUID mediaId, UUID ownerId) {
    String objectKey = mapper.findPendingObjectKey(mediaId, ownerId);
    if (objectKey == null) {
      throw new BusinessError("MEDIA_NOT_READY_OR_NOT_OWNED", "Media upload cannot be confirmed");
    }
    return objectKey;
  }

  @Override
  public boolean markReady(UUID mediaId, UUID ownerId, Instant uploadedAt) {
    return mapper.markReady(mediaId, ownerId, uploadedAt) == 1;
  }

  @Override
  public Optional<PublicMedia> findPublished(UUID mediaId) {
    return Optional.ofNullable(mapper.findPublished(mediaId))
        .map(row -> new PublicMedia(row.id(), row.objectKey(), row.contentType()));
  }
}
