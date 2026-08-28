package com.creatorhub.catalog.application.service;

import com.creatorhub.catalog.application.port.out.DigitalAssetScanner;
import com.creatorhub.catalog.application.port.out.DigitalAssetStore;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import com.creatorhub.sharedkernel.domain.AppClock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数字资产扫描编排服务；只有外部扫描明确通过后才把版本标记为 READY。 */
@Service
public class DigitalAssetScanService {

  private final DigitalAssetStore assets;
  private final DigitalAssetScanner scanner;

  public DigitalAssetScanService(DigitalAssetStore assets, DigitalAssetScanner scanner) {
    this.assets = assets;
    this.scanner = scanner;
  }

  /** 处理已进入扫描队列的版本，扫描失败进入隔离状态且不改变资产可交付资格。 */
  @Transactional
  public DigitalAssetVersion scan(UUID versionId) {
    DigitalAssetVersion scanning = assets.findVersion(Objects.requireNonNull(versionId));
    if (scanning.scanStatus()
        != com.creatorhub.catalog.domain.model.DigitalAssetScanStatus.SCANNING) {
      throw new IllegalStateException("Digital asset is not ready for scanning");
    }
    DigitalAssetScanner.ScanResult result = scanner.scan(scanning);
    if (result.passed()) {
      return assets.markScanPassed(scanning.id(), AppClock.now());
    }
    return assets.quarantine(scanning.id(), result.reason(), AppClock.now());
  }
}
