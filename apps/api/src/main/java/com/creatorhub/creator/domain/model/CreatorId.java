package com.creatorhub.creator.domain.model;

import com.creatorhub.sharedkernel.domain.StrongId;
import java.util.UUID;

/** Creator 聚合强类型 ID，防止与 UserId、ArtworkId 等 UUID 误用。 */
public final class CreatorId extends StrongId {

  private CreatorId(UUID value) {
    super(value);
  }

  public static CreatorId of(UUID value) {
    return new CreatorId(value);
  }

  public static CreatorId random() {
    return new CreatorId(StrongId.randomUuid());
  }
}
