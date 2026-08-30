package com.creatorhub.digitaldelivery.application.port.in;

import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import java.time.Instant;
import java.util.UUID;

/** 支付成功后的数字授权用例；只允许可信支付事件调用，不提供客户端直接授予权限的接口。 */
public interface DigitalEntitlementUseCases {

  EntitlementView getForBuyer(UUID buyerId, UUID orderId);

  Entitlement grantAfterPayment(
      UUID paymentEventId,
      UUID orderId,
      UUID buyerId,
      UUID artworkId,
      UUID assetVersionId,
      Instant paidAt);

  /** 买家可见的授权投影，不包含对象存储地址、下载 Token 或其他私有文件信息。 */
  record EntitlementView(
      UUID entitlementId,
      UUID orderId,
      UUID artworkId,
      UUID assetVersionId,
      String status,
      Instant grantedAt,
      Instant revokedAt,
      Instant createdAt,
      Instant updatedAt) {}
}
