package com.creatorhub.catalog.domain.model;

/** 数字资产版本扫描状态；只有 READY 版本允许绑定到已发布数字作品。 */
public enum DigitalAssetScanStatus {
  PENDING_SCAN,
  SCANNING,
  READY,
  QUARANTINED
}
