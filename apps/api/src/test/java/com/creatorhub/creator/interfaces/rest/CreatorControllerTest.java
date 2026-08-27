package com.creatorhub.creator.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.creator.application.port.in.CreatorUseCases.CreatorView;
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

/** 验证 Creator 公开查询、认证申请、方法角色和 owner 来源的 HTTP 契约。 */
@WebMvcTest(
    controllers = CreatorController.class,
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
class CreatorControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean CreatorUseCases creators;

  @Test
  void publicCreatorDetailDoesNotRequireAuthentication() throws Exception {
    UUID creatorId = UUID.randomUUID();
    when(creators.get(creatorId)).thenReturn(view(creatorId, UUID.randomUUID()));

    mvc.perform(get("/api/v1/creators/{creatorId}", creatorId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(creatorId.toString()))
        .andExpect(jsonPath("$.verified").value(false))
        .andExpect(jsonPath("$.artworkCount").value(0));
  }

  @Test
  void publicCreatorListSupportsCategoryFilter() throws Exception {
    when(creators.list("ART", null, 20))
        .thenReturn(new CreatorUseCases.CreatorPage(List.of(), null, false));

    mvc.perform(get("/api/v1/creators").param("category", "ART"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.hasMore").value(false));

    verify(creators).list("ART", null, 20);
  }

  @Test
  void authenticatedUserAppliesForCreatorUsingPrincipalAsOwner() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    when(creators.apply(eq(userId), any())).thenReturn(view(creatorId, userId));

    mvc.perform(
            post("/api/v1/me/creator")
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(
                            userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"displayName":"Alice Studio","slug":"alice-studio","category":"ILLUSTRATION","bio":"Bio","location":"Shanghai"}
                                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/creators/" + creatorId))
        .andExpect(jsonPath("$.slug").value("alice-studio"));

    verify(creators).apply(eq(userId), any());
  }

  @Test
  void creatorApplicationRequiresAuthentication() throws Exception {
    mvc.perform(
            post("/api/v1/me/creator")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"displayName":"Alice Studio","slug":"alice-studio","category":"ILLUSTRATION","bio":"Bio"}
                                """))
        .andExpect(status().isUnauthorized());
  }

  private static CreatorView view(UUID creatorId, UUID userId) {
    return new CreatorView(
        creatorId, userId, "alice-studio", "Alice Studio", "ILLUSTRATION", "Bio", "Shanghai");
  }
}
