package com.creatorhub.digitaldelivery.domain.repository;

import com.creatorhub.digitaldelivery.application.port.out.DigitalDownloadAsset;
import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import java.util.Optional;
import java.util.UUID;

/** 购买授权仓储端口；唯一订单授权保证重复支付事件不会重复产生权限。 */
public interface EntitlementRepository {

  Entitlement save(Entitlement entitlement);

  Optional<Entitlement> findByOrderId(UUID orderId);

  Optional<Entitlement> findAvailableByBuyer(UUID buyerId, UUID orderId);

  Optional<DigitalDownloadAsset> findDownloadAsset(UUID buyerId, UUID orderId);
}
