package com.creatorhub.catalog.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.catalog.application.port.in.ArtworkReviewUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases;
import com.creatorhub.catalog.application.port.in.ArtworkUseCases.ArtworkView;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.identity.infrastructure.security.CookieOriginValidationFilter;
import com.creatorhub.identity.infrastructure.security.JwtAuthenticationFilter;
import java.math.BigDecimal;
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

/** 验证作品写接口的 Creator 权限、嵌套金额校验和发布用例转发。 */
@WebMvcTest(
    controllers = ArtworkController.class,
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
class ArtworkControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean ArtworkUseCases artworks;
  @MockitoBean ArtworkReviewUseCases reviews;

  @Test
  void ordinaryUserCannotCreateArtwork() throws Exception {
    mvc.perform(
            post("/api/v1/artworks")
                .with(authentication(user(UUID.randomUUID(), "USER")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
        .andExpect(status().isForbidden());
  }

  @Test
  void creatorCreatesDraftUsingAuthenticatedPrincipal() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(artworks.create(eq(userId), any())).thenReturn(view(artworkId, ArtworkStatus.DRAFT));

    mvc.perform(
            post("/api/v1/artworks")
                .with(authentication(user(userId, "CREATOR")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(artworkId.toString()))
        .andExpect(jsonPath("$.status").value("DRAFT"));

    verify(artworks).create(eq(userId), any());
  }

  @Test
  void nestedPriceValidationRejectsNonPositiveAmount() throws Exception {
    String body = validBody().replace("\"amount\":\"99.00\"", "\"amount\":\"0.00\"");

    mvc.perform(
            post("/api/v1/artworks")
                .with(authentication(user(UUID.randomUUID(), "CREATOR")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void creatorPublishDelegatesToUseCase() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(artworks.publish(userId, artworkId)).thenReturn(view(artworkId, ArtworkStatus.PUBLISHED));

    mvc.perform(
            post("/api/v1/artworks/{artworkId}/publish", artworkId)
                .with(authentication(user(userId, "CREATOR")))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    verify(artworks).publish(userId, artworkId);
  }

  @Test
  void creatorCannotApproveArtworkReview() throws Exception {
    mvc.perform(
            post("/api/v1/artworks/{artworkId}/review/approve", UUID.randomUUID())
                .with(authentication(user(UUID.randomUUID(), "CREATOR")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"looks good\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanApproveArtworkWithAuditReason() throws Exception {
    UUID reviewerId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(reviews.approve(reviewerId, artworkId, "meets policy"))
        .thenReturn(view(artworkId, ArtworkStatus.APPROVED));

    mvc.perform(
            post("/api/v1/artworks/{artworkId}/review/approve", artworkId)
                .with(authentication(user(reviewerId, "ADMIN")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"meets policy\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    verify(reviews).approve(reviewerId, artworkId, "meets policy");
  }

  @Test
  void adminReviewRequiresReason() throws Exception {
    mvc.perform(
            post("/api/v1/artworks/{artworkId}/review/reject", UUID.randomUUID())
                .with(authentication(user(UUID.randomUUID(), "ADMIN")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId, String role) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  private static String validBody() {
    return """
        {"title":"Test artwork","description":"A sufficiently long description.","category":"ART","price":{"amount":"99.00","currency":"USD"},"stock":2,"mediaIds":[],"version":0}
        """;
  }

  private static ArtworkView view(UUID id, ArtworkStatus status) {
    return new ArtworkView(
        id,
        UUID.randomUUID(),
        "Test artwork",
        "A sufficiently long description.",
        "ART",
        new BigDecimal("99.00"),
        "USD",
        2,
        List.of(),
        status,
        0);
  }
}
