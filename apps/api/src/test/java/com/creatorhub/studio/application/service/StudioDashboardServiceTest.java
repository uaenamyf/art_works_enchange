package com.creatorhub.studio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.studio.application.port.in.StudioDashboardUseCase;
import com.creatorhub.studio.application.port.out.StudioDashboardQuery;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 Dashboard 应用服务只按认证用户查询只读指标。 */
class StudioDashboardServiceTest {

  @Test
  void forwardsAuthenticatedUserToDashboardQuery() {
    StudioDashboardQuery query = mock(StudioDashboardQuery.class);
    UUID userId = UUID.randomUUID();
    StudioDashboardUseCase.DashboardView expected =
        new StudioDashboardUseCase.DashboardView(Money.of("120.00", "CNY"), 3, 2, 8, 0);
    when(query.findByUserId(userId)).thenReturn(expected);

    StudioDashboardUseCase.DashboardView actual = new StudioDashboardService(query).get(userId);

    assertThat(actual).isEqualTo(expected);
    verify(query).findByUserId(userId);
  }
}
