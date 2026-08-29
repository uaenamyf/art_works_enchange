package com.creatorhub.digitaldelivery.application.port.out;

import com.creatorhub.digitaldelivery.domain.model.DownloadGrant;
import java.util.Optional;
import java.util.UUID;

/** 下载凭证仓储端口；只允许通过有效 Entitlement 创建和消费短期凭证。 */
public interface DownloadGrantRepository {

  DownloadGrant save(DownloadGrant grant);

  Optional<DownloadGrant> findById(UUID grantId);

  boolean markUsed(UUID grantId, java.time.Instant usedAt);

  int revokeByAssetVersion(UUID assetVersionId, java.time.Instant revokedAt);
}
