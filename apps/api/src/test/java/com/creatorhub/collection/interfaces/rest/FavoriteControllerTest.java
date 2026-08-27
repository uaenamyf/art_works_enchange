package com.creatorhub.collection.interfaces.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.collection.application.port.in.FavoriteUseCases;
import com.creatorhub.collection.application.port.in.FavoriteUseCases.FavoriteView;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证收藏写操作的认证入口、principal owner 来源和幂等响应协议。 */
@WebMvcTest(
    controllers = FavoriteController.class,
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
class FavoriteControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean FavoriteUseCases favorites;

  @Test
  void authenticatedUserCanFavoriteArtwork() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    when(favorites.favorite(userId, artworkId)).thenReturn(new FavoriteView(true, 1));

    mvc.perform(
            put("/api/v1/favorites/{artworkId}", artworkId)
                .with(authentication(user(userId)))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.favorited").value(true))
        .andExpect(jsonPath("$.favoriteCount").value(1));

    verify(favorites).favorite(userId, artworkId);
  }

  @Test
  void anonymousUserCannotFavoriteArtwork() throws Exception {
    mvc.perform(put("/api/v1/favorites/{artworkId}", UUID.randomUUID()).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserCanUnfavoriteArtworkIdempotently() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();

    mvc.perform(
            delete("/api/v1/favorites/{artworkId}", artworkId)
                .with(authentication(user(userId)))
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(favorites).unfavorite(userId, artworkId);
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
