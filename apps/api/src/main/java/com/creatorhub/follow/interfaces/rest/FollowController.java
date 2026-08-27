package com.creatorhub.follow.interfaces.rest;

import com.creatorhub.follow.application.port.in.FollowUseCases;
import com.creatorhub.follow.application.port.in.FollowUseCases.FollowState;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 关注 HTTP 适配器；用户身份只从认证 principal 获取，不能由请求体伪造。 */
@RestController
@RequestMapping("/api/v1")
public class FollowController {

  private final FollowUseCases follows;

  public FollowController(FollowUseCases follows) {
    this.follows = follows;
  }

  @org.springframework.web.bind.annotation.GetMapping("/me/follows")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<FollowUseCases.CreatorPage> listMyFollows(
      @AuthenticationPrincipal UUID userId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(follows.listMyFollows(userId, cursor, limit));
  }

  @org.springframework.web.bind.annotation.GetMapping("/creators/{creatorId}/followers")
  ResponseEntity<FollowUseCases.FollowerPage> listFollowers(
      @PathVariable UUID creatorId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(follows.listFollowers(creatorId, cursor, limit));
  }

  /** 关注 Creator；重复 PUT 返回当前状态和最新粉丝数。 */
  @PutMapping("/creators/{creatorId}/follow")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<FollowState> follow(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID creatorId) {
    return ResponseEntity.ok(follows.follow(userId, creatorId));
  }

  /** 取消关注；重复 DELETE 仍返回当前状态，保持客户端可重试。 */
  @DeleteMapping("/creators/{creatorId}/follow")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<Void> unfollow(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID creatorId) {
    follows.unfollow(userId, creatorId);
    return ResponseEntity.noContent().build();
  }
}
