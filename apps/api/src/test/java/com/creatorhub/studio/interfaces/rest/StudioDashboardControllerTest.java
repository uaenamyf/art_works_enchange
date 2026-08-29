package com.creatorhub.studio.interfaces.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.studio.application.port.in.StudioDashboardUseCase;
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

/** 验证 Dashboard 的 Creator 权限和四项指标响应映射。 */
@WebMvcTest(
    controllers = StudioDashboardController.class,
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
class StudioDashboardControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean StudioDashboardUseCase dashboard;

  @Test
  void creatorCanReadDashboard() throws Exception {
    UUID userId = UUID.randomUUID();
    when(dashboard.get(userId))
        .thenReturn(new StudioDashboardUseCase.DashboardView(Money.of("120.00", "CNY"), 3, 2, 8));

    mvc.perform(get("/api/v1/studio/dashboard").with(authentication(user(userId, "CREATOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revenue.amount").value("120.00"))
        .andExpect(jsonPath("$.revenue.currency").value("CNY"))
        .andExpect(jsonPath("$.orderCount").value(3))
        .andExpect(jsonPath("$.publishedArtworkCount").value(2))
        .andExpect(jsonPath("$.followerCount").value(8));

    verify(dashboard).get(eq(userId));
  }

  @Test
  void ordinaryUserCannotReadDashboard() throws Exception {
    mvc.perform(
            get("/api/v1/studio/dashboard").with(authentication(user(UUID.randomUUID(), "USER"))))
        .andExpect(status().isForbidden());
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId, String role) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
