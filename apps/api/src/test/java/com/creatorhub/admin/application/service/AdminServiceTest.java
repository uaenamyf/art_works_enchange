package com.creatorhub.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.admin.application.port.in.AdminUseCases;
import com.creatorhub.admin.application.port.out.AdminQuery;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 验证 Admin 应用服务只编排只读查询，不复制运营统计规则。 */
class AdminServiceTest {

  @Test
  void forwardsReviewQueueQuery() {
    AdminQuery query = mock(AdminQuery.class);
    AdminUseCases.ReviewQueuePage expected =
        new AdminUseCases.ReviewQueuePage(List.of(), null, false);
    when(query.findReviewQueue("cursor", 10)).thenReturn(expected);

    AdminUseCases.ReviewQueuePage actual = new AdminService(query).listReviewQueue("cursor", 10);

    assertThat(actual).isEqualTo(expected);
    verify(query).findReviewQueue("cursor", 10);
  }

  @Test
  void forwardsDashboardQuery() {
    AdminQuery query = mock(AdminQuery.class);
    AdminUseCases.DashboardView expected =
        new AdminUseCases.DashboardView(3, 1, 2, 1, Map.of("PUBLISHED", 2L), Map.of());
    when(query.findDashboard()).thenReturn(expected);

    AdminUseCases.DashboardView actual = new AdminService(query).dashboard();

    assertThat(actual).isEqualTo(expected);
    verify(query).findDashboard();
  }

  @Test
  void forwardsReviewHistoryQuery() {
    AdminQuery query = mock(AdminQuery.class);
    AdminUseCases.ReviewHistoryPage expected =
        new AdminUseCases.ReviewHistoryPage(List.of(), "next", true);
    when(query.findReviewHistory("cursor", 10)).thenReturn(expected);

    AdminUseCases.ReviewHistoryPage actual =
        new AdminService(query).listReviewHistory("cursor", 10);

    assertThat(actual).isEqualTo(expected);
    verify(query).findReviewHistory("cursor", 10);
  }
}
