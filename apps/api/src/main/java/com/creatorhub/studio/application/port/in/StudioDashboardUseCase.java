package com.creatorhub.studio.application.port.in;

import com.creatorhub.sharedkernel.domain.Money;
import java.util.UUID;

/** Creator Studio Dashboard 输入端口；只返回当前 Creator 的只读运营指标。 */
public interface StudioDashboardUseCase {

  DashboardView get(UUID userId);

  record DashboardView(
      Money revenue,
      long orderCount,
      long publishedArtworkCount,
      long followerCount,
      long digitalDownloadCount) {}
}
