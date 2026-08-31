package com.creatorhub.admin.application.service;

import com.creatorhub.admin.application.port.in.AdminUseCases;
import com.creatorhub.admin.application.port.out.AdminQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin 查询应用服务；只编排只读运营视图，不承载审核状态变更规则。 */
@Service
@Transactional(readOnly = true)
public class AdminService implements AdminUseCases {

  private final AdminQuery query;

  public AdminService(AdminQuery query) {
    this.query = query;
  }

  @Override
  public ReviewQueuePage listReviewQueue(String cursor, int limit) {
    return query.findReviewQueue(cursor, limit);
  }

  @Override
  public ReviewQueueItem getReviewArtwork(java.util.UUID artworkId) {
    return query.findReviewArtwork(artworkId);
  }

  @Override
  public ReviewHistoryPage listReviewHistory(String cursor, int limit) {
    return query.findReviewHistory(cursor, limit);
  }

  @Override
  public DashboardView dashboard() {
    return query.findDashboard();
  }
}
