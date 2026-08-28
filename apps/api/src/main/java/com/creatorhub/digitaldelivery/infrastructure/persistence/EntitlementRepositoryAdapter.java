package com.creatorhub.digitaldelivery.infrastructure.persistence;

import com.creatorhub.digitaldelivery.application.port.out.DigitalDownloadAsset;
import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 数字授权 PostgreSQL 适配器；只允许已扫描 READY 资产进入可用授权。 */
@Repository
public class EntitlementRepositoryAdapter implements EntitlementRepository {

  private final EntitlementMapper mapper;

  public EntitlementRepositoryAdapter(EntitlementMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public Entitlement save(Entitlement entitlement) {
    if (entitlement.status()
            == com.creatorhub.digitaldelivery.domain.model.EntitlementStatus.PENDING
        && mapper.isReadyAssetVersion(entitlement.assetVersionId()) == null) {
      throw new BusinessError("DIGITAL_ASSET_NOT_READY", "Digital asset is not ready");
    }
    EntitlementPersistenceRow row = toRow(entitlement);
    if (mapper.insert(row) == 1) return entitlement;
    return findByOrderId(entitlement.orderId())
        .orElseThrow(() -> new IllegalStateException("Entitlement insert conflict"));
  }

  @Override
  @Transactional
  public Entitlement grant(Entitlement entitlement) {
    if (mapper.isReadyAssetVersion(entitlement.assetVersionId()) == null) {
      throw new BusinessError("DIGITAL_ASSET_NOT_READY", "Digital asset is not ready");
    }
    EntitlementPersistenceRow row = toRow(entitlement);
    if (mapper.insert(row) == 1) return entitlement;
    return findByOrderId(entitlement.orderId())
        .orElseThrow(() -> new IllegalStateException("Entitlement insert conflict"));
  }

  @Override
  public Optional<Entitlement> findByOrderId(UUID orderId) {
    return Optional.ofNullable(mapper.findByOrderId(orderId))
        .map(EntitlementRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<Entitlement> findAvailableByBuyer(UUID buyerId, UUID orderId) {
    return Optional.ofNullable(mapper.findAvailableByBuyer(buyerId, orderId))
        .map(EntitlementRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<DigitalDownloadAsset> findDownloadAsset(UUID buyerId, UUID orderId) {
    return Optional.ofNullable(mapper.findDownloadAsset(buyerId, orderId));
  }

  private static EntitlementPersistenceRow toRow(Entitlement entitlement) {
    EntitlementPersistenceRow row = new EntitlementPersistenceRow();
    row.setId(entitlement.id());
    row.setOrderId(entitlement.orderId());
    row.setBuyerId(entitlement.buyerId());
    row.setArtworkId(entitlement.artworkId());
    row.setAssetVersionId(entitlement.assetVersionId());
    row.setStatus(entitlement.status());
    row.setGrantedAt(entitlement.grantedAt());
    row.setRevokedAt(entitlement.revokedAt());
    row.setRevokeReason(entitlement.revokeReason());
    row.setCreatedAt(entitlement.createdAt());
    row.setUpdatedAt(entitlement.updatedAt());
    return row;
  }

  private static Entitlement toDomain(EntitlementPersistenceRow row) {
    return Entitlement.reconstitute(
        row.getId(),
        row.getOrderId(),
        row.getBuyerId(),
        row.getArtworkId(),
        row.getAssetVersionId(),
        row.getStatus(),
        row.getGrantedAt(),
        row.getRevokedAt(),
        row.getRevokeReason(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }
}
