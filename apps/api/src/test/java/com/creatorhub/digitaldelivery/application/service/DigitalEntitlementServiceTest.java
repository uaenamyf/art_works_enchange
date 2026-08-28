package com.creatorhub.digitaldelivery.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证支付成功事件只产生 AVAILABLE 授权，重复事件由仓储幂等返回已有授权。 */
class DigitalEntitlementServiceTest {

  @Test
  void grantsAvailableEntitlementAfterTrustedPaymentEvent() {
    EntitlementRepository repository = mock(EntitlementRepository.class);
    Entitlement stored = entitlement();
    when(repository.grant(any(Entitlement.class))).thenReturn(stored);

    Entitlement result =
        new DigitalEntitlementService(repository)
            .grantAfterPayment(
                UUID.randomUUID(),
                stored.orderId(),
                stored.buyerId(),
                stored.artworkId(),
                stored.assetVersionId(),
                Instant.parse("2026-08-28T01:00:00Z"));

    assertThat(result.status())
        .isEqualTo(com.creatorhub.digitaldelivery.domain.model.EntitlementStatus.AVAILABLE);
  }

  @Test
  void repositoryReplayReturnsExistingEntitlement() {
    EntitlementRepository repository = mock(EntitlementRepository.class);
    Entitlement existing = entitlement();
    when(repository.grant(any(Entitlement.class))).thenReturn(existing);

    Entitlement result =
        new DigitalEntitlementService(repository)
            .grantAfterPayment(
                UUID.randomUUID(),
                existing.orderId(),
                existing.buyerId(),
                existing.artworkId(),
                existing.assetVersionId(),
                Instant.now());

    assertThat(result.id()).isEqualTo(existing.id());
  }

  private static Entitlement entitlement() {
    return Entitlement.available(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-08-28T00:00:00Z"));
  }
}
