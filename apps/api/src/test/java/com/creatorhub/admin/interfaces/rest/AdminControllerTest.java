package com.creatorhub.admin.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.admin.application.port.in.AdminUseCases;
import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 Admin 查询接口的角色隔离和只读响应结构。 */
@WebMvcTest(
    controllers = AdminController.class,
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
class AdminControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean AdminUseCases admin;

  @Test
  void ordinaryUserCannotReadDashboard() throws Exception {
    mvc.perform(get("/api/v1/admin/dashboard").with(authentication(user("USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanReadDashboard() throws Exception {
    when(admin.dashboard())
        .thenReturn(new AdminUseCases.DashboardView(4, 2, 3, 1, Map.of("PUBLISHED", 3L), Map.of()));

    mvc.perform(get("/api/v1/admin/dashboard").with(authentication(user("ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userCount").value(4))
        .andExpect(jsonPath("$.pendingReviewArtworkCount").value(1));
  }

  @Test
  void ordinaryUserCannotReadReviewQueue() throws Exception {
    mvc.perform(get("/api/v1/admin/artworks/review-queue").with(authentication(user("USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanReadReviewHistoryWithCursor() throws Exception {
    when(admin.listReviewHistory("cursor", 10))
        .thenReturn(new AdminUseCases.ReviewHistoryPage(List.of(), "next", true));

    mvc.perform(
            get("/api/v1/admin/artworks/review-history")
                .param("cursor", "cursor")
                .param("limit", "10")
                .with(authentication(user("ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.nextCursor").value("next"))
        .andExpect(jsonPath("$.hasMore").value(true));
  }

  @Test
  void ordinaryUserCannotReadReviewHistory() throws Exception {
    mvc.perform(get("/api/v1/admin/artworks/review-history").with(authentication(user("USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidReviewHistoryCursorReturnsBadRequestProblem() throws Exception {
    when(admin.listReviewHistory("bad-cursor", 20))
        .thenThrow(new BusinessError("INVALID_CURSOR", "Cursor is invalid"));

    mvc.perform(
            get("/api/v1/admin/artworks/review-history")
                .param("cursor", "bad-cursor")
                .with(authentication(user("ADMIN"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
  }

  private static UsernamePasswordAuthenticationToken user(String role) {
    return new UsernamePasswordAuthenticationToken(
        UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
