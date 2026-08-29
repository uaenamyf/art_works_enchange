package com.creatorhub.studio.application.port.out;

import com.creatorhub.studio.application.port.in.StudioDashboardUseCase.DashboardView;
import java.util.UUID;

/** Dashboard 只读查询端口；实现可以优化 SQL，但不能返回持久化行。 */
public interface StudioDashboardQuery {

  DashboardView findByUserId(UUID userId);
}
