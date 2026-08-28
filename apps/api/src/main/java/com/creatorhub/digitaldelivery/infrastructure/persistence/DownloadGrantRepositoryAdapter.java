package com.creatorhub.digitaldelivery.infrastructure.persistence;

import com.creatorhub.digitaldelivery.application.port.out.DownloadGrantRepository;
import com.creatorhub.digitaldelivery.domain.model.DownloadGrant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 下载凭证 PostgreSQL 适配器；原始 Token 永远不进入数据库。 */
@Repository
public class DownloadGrantRepositoryAdapter implements DownloadGrantRepository {

  private final DownloadGrantMapper mapper;

  public DownloadGrantRepositoryAdapter(DownloadGrantMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public DownloadGrant save(DownloadGrant grant) {
    DownloadGrantPersistenceRow row = new DownloadGrantPersistenceRow();
    row.setId(grant.id());
    row.setEntitlementId(grant.entitlementId());
    row.setTokenDigest(grant.tokenDigest());
    row.setExpiresAt(grant.expiresAt());
    row.setUsedAt(grant.usedAt());
    row.setRevokedAt(grant.revokedAt());
    row.setRequestIpHash(grant.requestIpHash());
    row.setUserAgentHash(grant.userAgentHash());
    row.setCreatedAt(grant.createdAt());
    if (mapper.insert(row) != 1) throw new IllegalStateException("Download grant insert failed");
    return grant;
  }

  @Override
  public Optional<DownloadGrant> findById(UUID grantId) {
    return Optional.ofNullable(mapper.findById(grantId))
        .map(DownloadGrantRepositoryAdapter::toDomain);
  }

  @Override
  public boolean markUsed(UUID grantId, java.time.Instant usedAt) {
    return mapper.markUsed(grantId, usedAt) == 1;
  }

  private static DownloadGrant toDomain(DownloadGrantPersistenceRow row) {
    return new DownloadGrant(
        row.getId(),
        row.getEntitlementId(),
        row.getTokenDigest(),
        row.getExpiresAt(),
        row.getUsedAt(),
        row.getRevokedAt(),
        row.getRequestIpHash(),
        row.getUserAgentHash(),
        row.getCreatedAt());
  }
}
