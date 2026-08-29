package com.creatorhub.studio.application.service;

import com.creatorhub.sharedkernel.error.BusinessError;
import com.creatorhub.studio.application.port.in.StudioDashboardUseCase;
import com.creatorhub.studio.application.port.out.StudioDashboardQuery;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Studio Dashboard 应用服务；只编排 Creator owner 校验和只读投影，不承载交易规则。 */
@Service
@Transactional(readOnly = true)
public class StudioDashboardService implements StudioDashboardUseCase {

  private final StudioDashboardQuery dashboard;

  public StudioDashboardService(StudioDashboardQuery dashboard) {
    this.dashboard = dashboard;
  }

  @Override
  public DashboardView get(UUID userId) {
    if (userId == null) {
      throw new BusinessError("CREATOR_NOT_FOUND", "Creator profile not found");
    }
    return dashboard.findByUserId(userId);
  }
}
