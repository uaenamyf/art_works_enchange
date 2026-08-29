package com.creatorhub.ordering.interfaces.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import com.creatorhub.ordering.application.port.in.OrderUseCases;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证创建订单的认证、幂等键、请求映射和 201 Location 协议。 */
@WebMvcTest(
    controllers = OrderController.class,
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
class OrderControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean OrderUseCases orders;

  @Test
  void authenticatedUserCanCreateOrder() throws Exception {
    UUID buyerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(orders.create(
            org.mockito.ArgumentMatchers.eq(buyerId),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("order-key")))
        .thenReturn(
            new OrderUseCases.OrderView(
                orderId,
                "CH202608270001",
                buyerId,
                artworkId,
                "Vase",
                BigDecimal.TEN,
                "CNY",
                2,
                BigDecimal.valueOf(20),
                OrderStatus.PENDING_PAYMENT,
                new ShippingAddress(
                    "Alice",
                    "1 Main Street",
                    null,
                    "Shanghai",
                    null,
                    "200000",
                    "CN",
                    "13800138000"),
                Instant.parse("2026-08-27T00:00:00Z"),
                Instant.parse("2026-08-27T00:00:00Z"),
                Instant.parse("2026-08-27T00:30:00Z")));

    mvc.perform(
            post("/api/v1/orders")
                .header("Idempotency-Key", "order-key")
                .with(authentication(user(buyerId)))
                .contentType("application/json")
                .content(
                    """
                    {"artworkId":"%s","quantity":2,"shippingAddress":{"recipientName":"Alice","line1":"1 Main Street","city":"Shanghai","postalCode":"200000","countryCode":"CN","phone":"13800138000"}}
                    """
                        .formatted(artworkId)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/orders/" + orderId));

    verify(orders)
        .create(
            org.mockito.ArgumentMatchers.eq(buyerId),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("order-key"));
  }

  @Test
  void anonymousUserCannotCreateOrder() throws Exception {
    mvc.perform(
            post("/api/v1/orders")
                .header("Idempotency-Key", "order-key")
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserCanListOrders() throws Exception {
    UUID buyerId = UUID.randomUUID();
    when(orders.list(buyerId, OrderStatus.PENDING_PAYMENT, "cursor", 10))
        .thenReturn(new OrderUseCases.OrderPage(java.util.List.of(), null, false));

    mvc.perform(
            get("/api/v1/orders")
                .param("status", "PENDING_PAYMENT")
                .param("cursor", "cursor")
                .param("limit", "10")
                .with(authentication(user(buyerId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.hasMore").value(false));

    verify(orders).list(buyerId, OrderStatus.PENDING_PAYMENT, "cursor", 10);
  }

  @Test
  void corsPreflightAllowsOrderIdempotencyKey() throws Exception {
    mvc.perform(
            options("/api/v1/orders")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,idempotency-key"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(
            header()
                .string(
                    "Access-Control-Allow-Headers",
                    org.hamcrest.Matchers.containsStringIgnoringCase("Idempotency-Key")));
  }

  @Test
  void authenticatedUserCanReadOwnOrder() throws Exception {
    UUID buyerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    when(orders.get(buyerId, orderId)).thenReturn(null);

    mvc.perform(get("/api/v1/orders/{orderId}", orderId).with(authentication(user(buyerId))))
        .andExpect(status().isOk());

    verify(orders).get(buyerId, orderId);
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
