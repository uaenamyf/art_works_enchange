package com.creatorhub.follow.interfaces.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creatorhub.config.GlobalExceptionHandler;
import com.creatorhub.config.SecurityConfig;
import com.creatorhub.follow.application.port.in.FollowUseCases;
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

/** 验证关注接口的认证入口、principal owner 来源和 204 取消协议。 */
@WebMvcTest(
    controllers = FollowController.class,
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
class FollowControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean FollowUseCases follows;

  @Test
  void authenticatedUserCanFollowCreator() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    when(follows.follow(userId, creatorId)).thenReturn(new FollowUseCases.FollowState(true, 1));

    mvc.perform(
            put("/api/v1/creators/{creatorId}/follow", creatorId)
                .with(authentication(user(userId)))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.following").value(true))
        .andExpect(jsonPath("$.followerCount").value(1));

    verify(follows).follow(userId, creatorId);
  }

  @Test
  void authenticatedUserCanUnfollowCreatorWithNoContent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();

    mvc.perform(
            delete("/api/v1/creators/{creatorId}/follow", creatorId)
                .with(authentication(user(userId)))
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(follows).unfollow(userId, creatorId);
  }

  @Test
  void anonymousUserCannotFollowCreator() throws Exception {
    mvc.perform(put("/api/v1/creators/{creatorId}/follow", UUID.randomUUID()).with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserCanListMyFollows() throws Exception {
    UUID userId = UUID.randomUUID();
    when(follows.listMyFollows(userId, "cursor", 10))
        .thenReturn(new FollowUseCases.CreatorPage(List.of(), null, false));

    mvc.perform(
            get("/api/v1/me/follows")
                .param("cursor", "cursor")
                .param("limit", "10")
                .with(authentication(user(userId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.hasMore").value(false));

    verify(follows).listMyFollows(userId, "cursor", 10);
  }

  @Test
  void anyoneCanListCreatorFollowers() throws Exception {
    UUID creatorId = UUID.randomUUID();
    when(follows.listFollowers(creatorId, null, 20))
        .thenReturn(new FollowUseCases.FollowerPage(List.of(), null, false));

    mvc.perform(get("/api/v1/creators/{creatorId}/followers", creatorId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.hasMore").value(false));

    verify(follows).listFollowers(creatorId, null, 20);
  }

  private static UsernamePasswordAuthenticationToken user(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
