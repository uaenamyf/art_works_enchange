package com.creatorhub.catalog.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** 公开媒体读取用例；只为已发布作品签发短期 URL，不暴露对象存储 key。 */
public interface MediaReadUseCases {

  MediaView getPublished(UUID mediaId);

  record MediaView(UUID id, String url, String contentType, Instant expiresAt) {}
}
