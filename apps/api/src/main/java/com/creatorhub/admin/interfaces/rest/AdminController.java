package com.creatorhub.admin.interfaces.rest;

import com.creatorhub.admin.application.port.in.AdminUseCases;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin 只读运营接口；审核批准与拒绝继续使用 Catalog 的既有写用例。 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final AdminUseCases admin;

  public AdminController(AdminUseCases admin) {
    this.admin = admin;
  }

  /** 返回待审核作品队列，作品详情和媒体关系均由查询层按审核状态过滤。 */
  @GetMapping("/artworks/review-queue")
  ResponseEntity<AdminUseCases.ReviewQueuePage> reviewQueue(
      @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(admin.listReviewQueue(cursor, limit));
  }

  /** 读取单件待审核作品，详情不存在或已离开队列时由应用层映射为资源不存在。 */
  @GetMapping("/artworks/review-queue/{artworkId}")
  ResponseEntity<AdminUseCases.ReviewQueueItem> reviewArtwork(@PathVariable UUID artworkId) {
    return ResponseEntity.ok(admin.getReviewArtwork(artworkId));
  }

  /** 返回审核决定历史，按审核时间和记录 ID 稳定倒序分页。 */
  @GetMapping("/artworks/review-history")
  ResponseEntity<AdminUseCases.ReviewHistoryPage> reviewHistory(
      @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(admin.listReviewHistory(cursor, limit));
  }

  /** 返回平台基础运营指标，不包含支付结算金额和用户写操作。 */
  @GetMapping("/dashboard")
  ResponseEntity<AdminUseCases.DashboardView> dashboard() {
    return ResponseEntity.ok(admin.dashboard());
  }
}
