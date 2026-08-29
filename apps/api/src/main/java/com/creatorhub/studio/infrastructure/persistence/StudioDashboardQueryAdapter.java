package com.creatorhub.studio.infrastructure.persistence;

import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.studio.application.port.in.StudioDashboardUseCase.DashboardView;
import com.creatorhub.studio.application.port.out.StudioDashboardQuery;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Dashboard 查询适配器；将 SQL 聚合结果显式映射为只读应用视图。 */
@Repository
public class StudioDashboardQueryAdapter implements StudioDashboardQuery {

  private final StudioDashboardMapper mapper;

  public StudioDashboardQueryAdapter(StudioDashboardMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public DashboardView findByUserId(UUID userId) {
    StudioDashboardMapper.StudioDashboardRow row = mapper.findByUserId(userId);
    if (row == null) {
      return new DashboardView(Money.zero(java.util.Currency.getInstance("CNY")), 0, 0, 0);
    }
    return new DashboardView(
        Money.of(row.revenue().toPlainString(), row.currency()),
        row.orderCount(),
        row.publishedArtworkCount(),
        row.followerCount());
  }
}
