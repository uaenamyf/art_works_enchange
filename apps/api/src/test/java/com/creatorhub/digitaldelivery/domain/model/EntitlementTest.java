package com.creatorhub.digitaldelivery.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证数字授权只能从待授权进入可用，撤回后不可恢复。 */
class EntitlementTest {

  @Test
  void pendingEntitlementBecomesAvailableOnce() {
    Entitlement entitlement = pending();
    Instant grantedAt = Instant.parse("2026-08-28T01:00:00Z");

    entitlement.makeAvailable(grantedAt);

    assertThat(entitlement.status()).isEqualTo(EntitlementStatus.AVAILABLE);
    assertThat(entitlement.grantedAt()).isEqualTo(grantedAt);
    assertThatThrownBy(() -> entitlement.makeAvailable(grantedAt.plusSeconds(1)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void revokedEntitlementCannotBeUsedAgain() {
    Entitlement entitlement = pending();
    entitlement.makeAvailable(Instant.parse("2026-08-28T01:00:00Z"));
    entitlement.revoke("asset recalled", Instant.parse("2026-08-28T02:00:00Z"));

    assertThat(entitlement.status()).isEqualTo(EntitlementStatus.REVOKED);
    assertThatThrownBy(() -> entitlement.makeAvailable(Instant.parse("2026-08-28T03:00:00Z")))
        .isInstanceOf(IllegalStateException.class);
  }

  private static Entitlement pending() {
    return Entitlement.pending(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-08-28T00:00:00Z"));
  }
}
