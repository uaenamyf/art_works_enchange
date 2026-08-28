package com.creatorhub.catalog.application.port.out;

import com.creatorhub.catalog.domain.model.DigitalAssetVersion;

/** 数字资产扫描输出端口；实现可接 ClamAV 或云扫描服务，不能在领域层依赖外部 SDK。 */
public interface DigitalAssetScanner {

  ScanResult scan(DigitalAssetVersion version);

  record ScanResult(boolean passed, String reason) {
    public ScanResult {
      if (!passed && (reason == null || reason.isBlank())) {
        throw new IllegalArgumentException("reason is required for failed scan");
      }
    }

    public static ScanResult clean() {
      return new ScanResult(true, null);
    }

    public static ScanResult quarantined(String reason) {
      return new ScanResult(false, reason);
    }
  }
}
