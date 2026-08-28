package com.creatorhub.digitaldelivery.application.port.in;

import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import java.time.Instant;
import java.util.UUID;

/** 支付成功后的数字授权用例；只允许可信支付事件调用，不提供客户端直接授予权限的接口。 */
public interface DigitalEntitlementUseCases {

  Entitlement grantAfterPayment(
      UUID paymentEventId,
      UUID orderId,
      UUID buyerId,
      UUID artworkId,
      UUID assetVersionId,
      Instant paidAt);
}
