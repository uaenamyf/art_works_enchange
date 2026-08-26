package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.in.MediaReadUseCases;
import com.creatorhub.catalog.application.port.out.MediaObjectStore;
import com.creatorhub.catalog.application.port.out.MediaStoragePort;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公开媒体查询应用服务；数据库状态门禁与短期签名共同保护非公开对象。 */
@Service
@Transactional(readOnly = true)
public class MediaReadService implements MediaReadUseCases {

  private final MediaObjectStore objects;
  private final MediaStoragePort storage;
  private final long urlTtlSeconds;

  public MediaReadService(
      MediaObjectStore objects,
      MediaStoragePort storage,
      @Value("${creatorhub.storage.media-url-ttl-seconds:300}") long urlTtlSeconds) {
    this.objects = objects;
    this.storage = storage;
    this.urlTtlSeconds = urlTtlSeconds;
  }

  @Override
  public MediaView getPublished(UUID mediaId) {
    MediaObjectStore.PublicMedia media =
        objects
            .findPublished(mediaId)
            .orElseThrow(() -> new BusinessError("MEDIA_NOT_FOUND", "Media not found"));
    Instant expiresAt = Instant.now().plus(Duration.ofSeconds(urlTtlSeconds));
    return new MediaView(
        media.id(),
        storage.presignGet(media.objectKey(), media.contentType(), expiresAt),
        media.contentType(),
        expiresAt);
  }
}
