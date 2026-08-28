package com.creatorhub.digitaldelivery.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证下载凭证只在未使用、未撤销且未过期时有效，并拒绝非 SHA-256 摘要。 */
class DownloadGrantTest {

  @Test
  void grantIsUsableOnlyBeforeExpiry() {
    Instant createdAt = Instant.parse("2026-08-28T00:00:00Z");
    DownloadGrant grant =
        DownloadGrant.issue(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "a".repeat(64),
            createdAt.plusSeconds(300),
            null,
            null,
            createdAt);

    assertThat(grant.usableAt(createdAt.plusSeconds(299))).isTrue();
    assertThat(grant.usableAt(createdAt.plusSeconds(300))).isFalse();
  }

  @Test
  void rejectsInvalidTokenDigest() {
    assertThatThrownBy(
            () ->
                DownloadGrant.issue(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "plain-token",
                    Instant.parse("2026-08-28T00:05:00Z"),
                    null,
                    null,
                    Instant.parse("2026-08-28T00:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tokenDigest must be a hexadecimal digest");
  }
}
