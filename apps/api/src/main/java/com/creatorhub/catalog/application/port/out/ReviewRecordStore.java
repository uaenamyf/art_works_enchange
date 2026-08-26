package com.creatorhub.catalog.application.port.out;

import com.creatorhub.catalog.domain.model.ArtworkStatus;
import java.time.Instant;
import java.util.UUID;

/** 审核审计记录输出端口，隔离应用层与 MyBatis 持久化实现。 */
public interface ReviewRecordStore {

  void append(
      UUID artworkId, UUID reviewerId, ArtworkStatus decision, String reason, Instant createdAt);
}
