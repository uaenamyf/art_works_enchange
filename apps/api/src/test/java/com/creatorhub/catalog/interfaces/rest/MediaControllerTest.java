package com.creatorhub.catalog.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.catalog.application.port.in.MediaUploadUseCases;
import com.creatorhub.catalog.application.port.in.MediaUploadUseCases.UploadView;
import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import java.time.Instant;
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

/** 验证媒体上传请求的 Creator 权限、输入校验和完成确认协议。 */
@WebMvcTest(
    controllers = MediaController.class,
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
class MediaControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean MediaUploadUseCases uploads;

  @Test
  void ordinaryUserCannotCreateUploadRequest() throws Exception {
    mvc.perform(
            post("/api/v1/media/upload-requests")
                .with(authentication(user("USER")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
        .andExpect(status().isForbidden());
  }

  @Test
  void creatorReceivesPresignedUploadTarget() throws Exception {
    UUID mediaId = UUID.randomUUID();
    when(uploads.createUpload(any(), any()))
        .thenReturn(
            new UploadView(
                mediaId,
                "http://localhost:9000/creatorhub-media/object",
                "PUT",
                "image/webp",
                Instant.parse("2026-08-26T01:00:00Z")));

    mvc.perform(
            post("/api/v1/media/upload-requests")
                .with(authentication(user("CREATOR")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mediaId").value(mediaId.toString()))
        .andExpect(jsonPath("$.method").value("PUT"))
        .andExpect(jsonPath("$.contentType").value("image/webp"));

    verify(uploads).createUpload(any(), any());
  }

  @Test
  void unsupportedMimeTypeIsRejected() throws Exception {
    mvc.perform(
            post("/api/v1/media/upload-requests")
                .with(authentication(user("CREATOR")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody().replace("image/webp", "image/svg+xml")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void creatorCanConfirmUpload() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID mediaId = UUID.randomUUID();

    mvc.perform(
            post("/api/v1/media/{mediaId}/confirm", mediaId)
                .with(authentication(user(ownerId, "CREATOR")))
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(uploads).confirmUpload(ownerId, mediaId);
  }

  private static UsernamePasswordAuthenticationToken user(String role) {
    return user(UUID.randomUUID(), role);
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId, String role) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  private static String validBody() {
    return """
        {"fileName":"cover.webp","contentType":"image/webp","sizeBytes":1024,"purpose":"ARTWORK"}
        """;
  }
}
