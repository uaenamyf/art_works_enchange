package com.creatorhub.catalog.interfaces.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.catalog.application.port.in.DigitalAssetRecallUseCase;
import com.creatorhub.catalog.application.port.in.DigitalAssetUploadUseCases;
import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证数字资产撤回只允许 Creator，并支持 Bearer 请求不携带 CSRF Token。 */
@WebMvcTest(
    controllers = DigitalAssetController.class,
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
class DigitalAssetControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean DigitalAssetUploadUseCases uploads;
  @MockitoBean DigitalAssetRecallUseCase recalls;

  @Test
  void creatorCanRecallWithoutCsrfToken() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();

    mvc.perform(
            post("/api/v1/digital-assets/{versionId}/recall", versionId)
                .with(authentication(user(ownerId, "CREATOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"版权调整\"}"))
        .andExpect(status().isNoContent());

    verify(recalls).recall(eq(ownerId), eq(versionId), eq("版权调整"));
  }

  @Test
  void ordinaryUserCannotRecallAsset() throws Exception {
    mvc.perform(
            post("/api/v1/digital-assets/{versionId}/recall", UUID.randomUUID())
                .with(authentication(user(UUID.randomUUID(), "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"版权调整\"}"))
        .andExpect(status().isForbidden());
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId, String role) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
