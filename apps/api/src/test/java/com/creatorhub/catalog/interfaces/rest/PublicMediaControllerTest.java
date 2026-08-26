package com.creatorhub.catalog.interfaces.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.catalog.application.port.in.MediaReadUseCases;
import com.creatorhub.catalog.application.port.in.MediaReadUseCases.MediaView;
import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证公开媒体 URL 可以匿名读取，并转发到媒体读取用例。 */
@WebMvcTest(
    controllers = PublicMediaController.class,
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
class PublicMediaControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean MediaReadUseCases media;

  @Test
  void anonymousUserCanReadPublishedMediaUrl() throws Exception {
    UUID mediaId = UUID.randomUUID();
    when(media.getPublished(mediaId))
        .thenReturn(
            new MediaView(
                mediaId,
                "http://localhost:9000/media/read",
                "image/webp",
                Instant.parse("2026-08-26T01:05:00Z")));

    mvc.perform(get("/api/v1/media/{mediaId}", mediaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(mediaId.toString()))
        .andExpect(jsonPath("$.contentType").value("image/webp"));

    verify(media).getPublished(mediaId);
  }
}
