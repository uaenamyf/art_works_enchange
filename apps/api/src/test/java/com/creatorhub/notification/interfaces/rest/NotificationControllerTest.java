package com.creatorhub.notification.interfaces.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import com.creatorhub.notification.application.port.in.NotificationUseCases;
import com.creatorhub.notification.domain.model.Notification;
import com.creatorhub.notification.domain.model.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证通知接口只使用认证用户身份，并正确转发未读筛选和已读操作。 */
@WebMvcTest(
    controllers = NotificationController.class,
    properties = {
      "creatorhub.security.jwt.secret=change-me-please-change-me-please-32bytes",
      "creatorhub.security.jwt.issuer=creatorhub",
      "creatorhub.cors.allowed-origins=http://localhost:3000"
    })
@Import({
  SecurityConfig.class,
  GlobalExceptionHandler.class,
  JwtAuthenticationFilter.class,
  CookieOriginValidationFilter.class
})
class NotificationControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean NotificationUseCases notifications;

  @Test
  void authenticatedUserListsOwnNotifications() throws Exception {
    UUID userId = UUID.randomUUID();
    when(notifications.list(userId, "cursor-value", 10, true))
        .thenReturn(
            new NotificationUseCases.NotificationPage(
                List.of(notification(userId)), "next", true, 1));

    mvc.perform(
            get("/api/v1/notifications")
                .param("cursor", "cursor-value")
                .param("limit", "10")
                .param("unreadOnly", "true")
                .with(authentication(user(userId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("订单已创建"))
        .andExpect(jsonPath("$.unreadCount").value(1))
        .andExpect(jsonPath("$.nextCursor").value("next"));

    verify(notifications).list(userId, "cursor-value", 10, true);
  }

  @Test
  void authenticatedUserMarksNotificationRead() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    mvc.perform(
            post("/api/v1/notifications/{notificationId}/read", notificationId)
                .with(authentication(user(userId)))
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(notifications).markRead(userId, notificationId);
  }

  @Test
  void anonymousUserCannotListNotifications() throws Exception {
    mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
  }

  private static Notification notification(UUID userId) {
    return new Notification(
        UUID.randomUUID(),
        userId,
        UUID.randomUUID(),
        NotificationType.ORDER_CREATED,
        "订单已创建",
        "你的订单已创建。",
        "/orders/order-1",
        null,
        Instant.parse("2026-08-28T00:00:00Z"));
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
