package com.creatorhub.catalog.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证未配置病毒扫描服务时数字文件默认隔离，不会被误标记为安全。 */
class UnavailableDigitalAssetScannerTest {

  @Test
  void failsClosed() {
    DigitalAssetVersion version =
        DigitalAssetVersion.pendingScan(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            "bundle.zip",
            "private/digital/bundle.zip",
            "application/zip",
            1024,
            "a".repeat(64),
            Instant.now());

    var result = new UnavailableDigitalAssetScanner().scan(version);

    assertThat(result.passed()).isFalse();
    assertThat(result.reason()).isEqualTo("DIGITAL_ASSET_SCANNER_NOT_CONFIGURED");
  }
}
