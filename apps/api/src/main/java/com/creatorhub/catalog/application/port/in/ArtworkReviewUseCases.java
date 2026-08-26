package com.creatorhub.catalog.application.port.in;

import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import java.time.Instant;
import java.util.UUID;

/** 审核后台输入端口；审核决定只能由后台身份触发，Creator 不能自审。 */
public interface ArtworkReviewUseCases {

  ArtworkView approve(UUID reviewerId, UUID artworkId, String reason);

  ArtworkView reject(UUID reviewerId, UUID artworkId, String reason);

  /** 审核记录只读快照，供后台审计和申诉流程使用。 */
  record ReviewView(
      UUID id,
      UUID artworkId,
      UUID reviewerId,
      ArtworkStatus decision,
      String reason,
      Instant createdAt) {}
}
