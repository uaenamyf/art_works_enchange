package com.creatorhub.catalog.infrastructure.security;

import com.creatorhub.catalog.application.port.out.DigitalAssetScanner;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import org.springframework.stereotype.Component;

/** 未接入真实病毒扫描服务时的安全降级实现；所有文件默认隔离，禁止误放行。 */
@Component
public class UnavailableDigitalAssetScanner implements DigitalAssetScanner {

  @Override
  public ScanResult scan(DigitalAssetVersion version) {
    return ScanResult.quarantined("DIGITAL_ASSET_SCANNER_NOT_CONFIGURED");
  }
}
