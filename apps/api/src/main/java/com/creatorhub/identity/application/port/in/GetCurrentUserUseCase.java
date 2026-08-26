package com.creatorhub.identity.application.port.in;

import java.util.UUID;

/** 获取当前认证用户最小公开概要的输入端口。 */
public interface GetCurrentUserUseCase {

  UserSummary get(UUID userId);

  /** 与 OpenAPI UserSummary 对齐的只读身份快照。 */
  record UserSummary(UUID id, String displayName, String avatarUrl, String role) {}
}
