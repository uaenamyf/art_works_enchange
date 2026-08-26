package com.creatorhub.catalog.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** 媒体对象状态输出端口，记录上传请求与用户归属。 */
public interface MediaObjectStore {

  void createPending(
      UUID mediaId,
      UUID ownerId,
      String objectKey,
      String contentType,
      long sizeBytes,
      Instant expiresAt,
      Instant createdAt);

  String objectKeyForReadyCandidate(UUID mediaId, UUID ownerId);

  boolean markReady(UUID mediaId, UUID ownerId, Instant uploadedAt);

  Optional<PublicMedia> findPublished(UUID mediaId);

  record PublicMedia(UUID id, String objectKey, String contentType) {}
}
