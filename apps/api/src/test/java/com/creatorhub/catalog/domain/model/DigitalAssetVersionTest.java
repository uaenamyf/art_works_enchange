package com.creatorhub.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证数字资产版本只能从待扫描开始，并拒绝无效哈希。 */
class DigitalAssetVersionTest {

  @Test
  void newVersionStartsPendingScan() {
    DigitalAssetVersion version =
        DigitalAssetVersion.pendingScan(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            "font.zip",
            "private/digital/font.zip",
            "application/zip",
            1024,
            "A".repeat(64),
            Instant.parse("2026-08-28T00:00:00Z"));

    assertThat(version.scanStatus()).isEqualTo(DigitalAssetScanStatus.PENDING_SCAN);
    assertThat(version.sha256()).isEqualTo("a".repeat(64));
  }

  @Test
  void rejectsInvalidHash() {
    assertThatThrownBy(
            () ->
                DigitalAssetVersion.pendingScan(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    1,
                    "font.zip",
                    "private/digital/font.zip",
                    "application/zip",
                    1024,
                    "not-a-hash",
                    Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sha256 must be a hexadecimal digest");
  }
}
