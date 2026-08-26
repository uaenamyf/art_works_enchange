package com.creatorhub.catalog.domain.model;

import com.creatorhub.sharedkernel.domain.StrongId;
import java.util.UUID;

/** 作品聚合的强类型标识，跨上下文只传递该 ID，不暴露 Catalog 持久化对象。 */
public final class ArtworkId extends StrongId {

  private ArtworkId(UUID value) {
    super(value);
  }

  public static ArtworkId of(UUID value) {
    return new ArtworkId(value);
  }

  public static ArtworkId random() {
    return new ArtworkId(StrongId.randomUuid());
  }
}
