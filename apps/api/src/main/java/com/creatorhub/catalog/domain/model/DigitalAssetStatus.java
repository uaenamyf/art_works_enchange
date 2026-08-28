package com.creatorhub.catalog.domain.model;

/** 数字资产整体状态；撤回或封禁后不得继续签发下载凭证。 */
public enum DigitalAssetStatus {
  DRAFT,
  READY,
  RECALLED,
  BLOCKED
}
