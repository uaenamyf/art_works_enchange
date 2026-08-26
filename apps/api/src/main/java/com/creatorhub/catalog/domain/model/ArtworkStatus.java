/**
 * 作品生命周期与审核状态。
 *
 * <p>审核状态和销售状态合并在同一状态机中，公开查询只能返回 PUBLISHED；任何审核失败或封禁状态都不能直接公开。
 */
package com.creatorhub.catalog.domain.model;

public enum ArtworkStatus {
  DRAFT,
  PENDING_REVIEW,
  APPROVED,
  REJECTED,
  PUBLISHED,
  SOLD_OUT,
  ARCHIVED,
  BLOCKED
}
