package com.creatorhub.admin.application.port.out;

import com.creatorhub.admin.application.port.in.AdminUseCases.DashboardView;
import com.creatorhub.admin.application.port.in.AdminUseCases.ReviewHistoryPage;
import com.creatorhub.admin.application.port.in.AdminUseCases.ReviewQueueItem;
import com.creatorhub.admin.application.port.in.AdminUseCases.ReviewQueuePage;
import java.util.UUID;

/** Admin 查询输出端口；应用层只依赖脱离数据库行的只读视图。 */
public interface AdminQuery {

  ReviewQueuePage findReviewQueue(String cursor, int limit);

  ReviewQueueItem findReviewArtwork(UUID artworkId);

  ReviewHistoryPage findReviewHistory(String cursor, int limit);

  DashboardView findDashboard();
}
