package com.creatorhub.digitaldelivery.interfaces.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.digitaldelivery.application.port.in.DigitalEntitlementUseCases;
import com.creatorhub.digitaldelivery.application.port.in.DigitalEntitlementUseCases.EntitlementView;
import com.creatorhub.digitaldelivery.application.service.DigitalDownloadService;
import com.creatorhub.digitaldelivery.application.service.DigitalDownloadService.DownloadView;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
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

/** 验证数字下载接口的买家认证、短期响应字段和匿名访问拒绝。 */
@WebMvcTest(
    controllers = DigitalDownloadController.class,
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
class DigitalDownloadControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean DigitalDownloadService downloads;
  @MockitoBean DigitalEntitlementUseCases entitlements;

  @Test
  void authenticatedBuyerReceivesShortLivedGrant() throws Exception {
    UUID buyerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    when(downloads.createGrant(buyerId, orderId, "127.0.0.1", "MockMvc"))
        .thenReturn(
            new DownloadView(
                UUID.randomUUID(),
                "bundle.zip",
                "application/zip",
                1024,
                1,
                "https://private.example/download",
                Instant.parse("2026-08-28T01:00:00Z")));

    mvc.perform(
            post("/api/v1/orders/{orderId}/download-grants", orderId)
                .with(authentication(user(buyerId)))
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      request.addHeader("User-Agent", "MockMvc");
                      return request;
                    })
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.grantId").isNotEmpty())
        .andExpect(jsonPath("$.fileName").value("bundle.zip"))
        .andExpect(jsonPath("$.downloadUrl").value("https://private.example/download"));

    verify(downloads).createGrant(buyerId, orderId, "127.0.0.1", "MockMvc");
  }

  @Test
  void anonymousBuyerCannotRequestGrant() throws Exception {
    mvc.perform(post("/api/v1/orders/{orderId}/download-grants", UUID.randomUUID()).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedBuyerCanReadRedactedEntitlement() throws Exception {
    UUID buyerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    when(entitlements.getForBuyer(buyerId, orderId))
        .thenReturn(
            new EntitlementView(
                UUID.randomUUID(),
                orderId,
                artworkId,
                versionId,
                "AVAILABLE",
                Instant.parse("2026-08-28T00:00:00Z"),
                null,
                Instant.parse("2026-08-28T00:00:00Z"),
                Instant.parse("2026-08-28T00:00:00Z")));

    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/orders/{orderId}/entitlement", orderId)
                .with(authentication(user(buyerId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
        .andExpect(jsonPath("$.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.objectKey").doesNotExist())
        .andExpect(jsonPath("$.downloadUrl").doesNotExist());

    verify(entitlements).getForBuyer(buyerId, orderId);
  }

  @Test
  void nonBuyerReceivesNotFoundWhenReadingEntitlement() throws Exception {
    UUID buyerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    when(entitlements.getForBuyer(buyerId, orderId))
        .thenThrow(
            new com.creatorhub.sharedkernel.error.BusinessError(
                "DIGITAL_ENTITLEMENT_NOT_FOUND", "Digital entitlement was not found"));

    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/orders/{orderId}/entitlement", orderId)
                .with(authentication(user(buyerId))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("DIGITAL_ENTITLEMENT_NOT_FOUND"));

    verify(entitlements).getForBuyer(buyerId, orderId);
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
