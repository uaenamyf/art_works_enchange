package com.creatorhub.studio.interfaces.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import com.creatorhub.ordering.application.port.in.OrderUseCases;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.studio.application.port.in.StudioOrderUseCase;
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

/** 验证 Studio 订单接口的 Creator 权限、筛选参数和状态变更转发。 */
@WebMvcTest(
    controllers = StudioOrderController.class,
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
class StudioOrderControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean StudioOrderUseCase orders;

  @Test
  void creatorCanListAndUpdateOrderStatus() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    when(orders.list(userId, OrderStatus.PAID, "cursor", 10))
        .thenReturn(new OrderUseCases.OrderPage(List.of(), null, false));

    mvc.perform(
            get("/api/v1/studio/orders")
                .param("status", "PAID")
                .param("cursor", "cursor")
                .param("limit", "10")
                .with(authentication(user(userId, "CREATOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
    verify(orders).list(userId, OrderStatus.PAID, "cursor", 10);

    mvc.perform(
            patch("/api/v1/studio/orders/{orderId}/status", orderId)
                .with(authentication(user(userId, "CREATOR")))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"PROCESSING\"}"))
        .andExpect(status().isOk());
    verify(orders)
        .updateStatus(eq(userId), eq(orderId), eq(OrderStatus.PROCESSING), eq(null), eq(null));
  }

  @Test
  void ordinaryUserCannotListStudioOrders() throws Exception {
    mvc.perform(get("/api/v1/studio/orders").with(authentication(user(UUID.randomUUID(), "USER"))))
        .andExpect(status().isForbidden());
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId, String role) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
