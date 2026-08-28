package com.creatorhub.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.application.port.out.DigitalAssetScanner;
import com.creatorhub.catalog.application.port.out.DigitalAssetStore;
import com.creatorhub.catalog.domain.model.DigitalAssetScanStatus;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证扫描编排只允许通过结果进入 READY，失败结果必须隔离。 */
class DigitalAssetScanServiceTest {

  @Test
  void passedScanMarksVersionReady() {
    DigitalAssetStore assets = mock(DigitalAssetStore.class);
    DigitalAssetScanner scanner = mock(DigitalAssetScanner.class);
    DigitalAssetVersion scanning = version(DigitalAssetScanStatus.SCANNING);
    DigitalAssetVersion ready = version(DigitalAssetScanStatus.READY);
    when(assets.findVersion(scanning.id())).thenReturn(scanning);
    when(scanner.scan(scanning)).thenReturn(DigitalAssetScanner.ScanResult.clean());
    when(assets.markScanPassed(any(), any())).thenReturn(ready);

    DigitalAssetVersion result = new DigitalAssetScanService(assets, scanner).scan(scanning.id());

    assertThat(result.scanStatus()).isEqualTo(DigitalAssetScanStatus.READY);
    verify(assets)
        .markScanPassed(org.mockito.ArgumentMatchers.eq(scanning.id()), any(Instant.class));
  }

  @Test
  void failedScanQuarantinesVersion() {
    DigitalAssetStore assets = mock(DigitalAssetStore.class);
    DigitalAssetScanner scanner = mock(DigitalAssetScanner.class);
    DigitalAssetVersion scanning = version(DigitalAssetScanStatus.SCANNING);
    DigitalAssetVersion quarantined = version(DigitalAssetScanStatus.QUARANTINED);
    when(assets.findVersion(scanning.id())).thenReturn(scanning);
    when(scanner.scan(scanning)).thenReturn(DigitalAssetScanner.ScanResult.quarantined("malware"));
    when(assets.quarantine(any(), any(), any())).thenReturn(quarantined);

    DigitalAssetVersion result = new DigitalAssetScanService(assets, scanner).scan(scanning.id());

    assertThat(result.scanStatus()).isEqualTo(DigitalAssetScanStatus.QUARANTINED);
    verify(assets)
        .quarantine(
            org.mockito.ArgumentMatchers.eq(scanning.id()),
            org.mockito.ArgumentMatchers.eq("malware"),
            any(Instant.class));
  }

  private static DigitalAssetVersion version(DigitalAssetScanStatus status) {
    return new DigitalAssetVersion(
        UUID.randomUUID(),
        UUID.randomUUID(),
        1,
        "asset.zip",
        "private/digital/asset.zip",
        "application/zip",
        1024,
        "a".repeat(64),
        status,
        null,
        Instant.parse("2026-08-28T00:00:00Z"),
        status == DigitalAssetScanStatus.READY || status == DigitalAssetScanStatus.QUARANTINED
            ? Instant.parse("2026-08-28T00:01:00Z")
            : null);
  }
}
