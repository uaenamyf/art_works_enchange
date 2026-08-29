package com.creatorhub.digitaldelivery.domain.repository;

import com.creatorhub.digitaldelivery.application.port.out.DigitalDownloadAsset;
import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import java.util.Optional;
import java.util.UUID;

/** 购买授权仓储端口；唯一订单授权保证重复支付事件不会重复产生权限。 */
public interface EntitlementRepository {

  Entitlement save(Entitlement entitlement);

  /** 以订单唯一约束幂等创建支付成功后的可用授权。 */
  Entitlement grant(Entitlement entitlement);

  Optional<Entitlement> findByOrderId(UUID orderId);

  Optional<Entitlement> findAvailableByBuyer(UUID buyerId, UUID orderId);

  Optional<DigitalDownloadAsset> findDownloadAsset(UUID buyerId, UUID orderId);

  int revokeAvailableByAssetVersion(
      UUID assetVersionId, String reason, java.time.Instant revokedAt);
}
