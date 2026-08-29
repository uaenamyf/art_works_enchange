package com.creatorhub.studio.interfaces.rest;

import com.creatorhub.studio.application.port.in.StudioDashboardUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Studio Dashboard REST 适配器；认证用户是唯一查询 owner。 */
@RestController
@RequestMapping("/api/v1/studio/dashboard")
public class StudioDashboardController {

  private final StudioDashboardUseCase dashboard;

  public StudioDashboardController(StudioDashboardUseCase dashboard) {
    this.dashboard = dashboard;
  }

  /** 读取当前 Creator 的四项 Dashboard 指标。 */
  @GetMapping
  @PreAuthorize("hasRole('CREATOR')")
  ResponseEntity<DashboardResponse> get(@AuthenticationPrincipal UUID userId) {
    return ResponseEntity.ok(DashboardResponse.from(dashboard.get(userId)));
  }

  /** API 金额 DTO；禁止直接暴露领域 Money 的辅助行为方法。 */
  record DashboardResponse(
      MoneyResponse revenue, long orderCount, long publishedArtworkCount, long followerCount) {
    static DashboardResponse from(StudioDashboardUseCase.DashboardView view) {
      return new DashboardResponse(
          new MoneyResponse(
              view.revenue().amount().toPlainString(), view.revenue().currency().getCurrencyCode()),
          view.orderCount(),
          view.publishedArtworkCount(),
          view.followerCount());
    }
  }

  record MoneyResponse(String amount, String currency) {}
}
