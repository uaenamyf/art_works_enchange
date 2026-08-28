package com.creatorhub.digitaldelivery.domain.model;

/** 数字购买授权状态；授权状态独立于订单状态，撤回后不得再签发下载凭证。 */
public enum EntitlementStatus {
  PENDING,
  AVAILABLE,
  REVOKED
}
