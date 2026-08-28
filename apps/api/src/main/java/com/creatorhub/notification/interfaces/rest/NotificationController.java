package com.creatorhub.notification.interfaces.rest;

import com.creatorhub.notification.application.port.in.NotificationUseCases;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 通知 HTTP 适配器；用户身份只从认证 principal 获取，路径参数不能替换所有者。 */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

  private final NotificationUseCases notifications;

  public NotificationController(NotificationUseCases notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  ResponseEntity<NotificationUseCases.NotificationPage> list(
      @AuthenticationPrincipal UUID userId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "false") boolean unreadOnly) {
    return ResponseEntity.ok(notifications.list(userId, cursor, limit, unreadOnly));
  }

  @PostMapping("/{notificationId}/read")
  ResponseEntity<Void> markRead(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID notificationId) {
    notifications.markRead(userId, notificationId);
    return ResponseEntity.noContent().build();
  }
}
