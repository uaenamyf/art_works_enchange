package com.creatorhub.identity.interfaces.rest;

import com.creatorhub.identity.application.port.in.GetCurrentUserUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前认证用户 HTTP 适配器，仅把安全上下文 UserId 转换为查询用例输入。 */
@RestController
@RequestMapping("/api/v1/me")
public class CurrentUserController {

  private final GetCurrentUserUseCase currentUser;

  public CurrentUserController(GetCurrentUserUseCase currentUser) {
    this.currentUser = currentUser;
  }

  /** 返回数据库中的最新用户概要，避免依赖 JWT 内可能过时的展示名或角色。 */
  @GetMapping
  ResponseEntity<GetCurrentUserUseCase.UserSummary> get(@AuthenticationPrincipal UUID userId) {
    return ResponseEntity.ok(currentUser.get(userId));
  }
}
