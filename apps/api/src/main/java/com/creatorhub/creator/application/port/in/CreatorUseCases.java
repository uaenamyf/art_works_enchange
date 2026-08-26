package com.creatorhub.creator.application.port.in;

import java.util.UUID;

/** Creator 申请、更新与公开查询用例集合。 */
public interface CreatorUseCases {

  CreatorView apply(UUID userId, ApplyCommand command);

  CreatorView update(UUID userId, UpdateCommand command);

  CreatorView get(UUID creatorId);

  CreatorView getByUserId(UUID userId);

  /** 申请命令中的 owner 始终来自认证上下文，不允许客户端传入。 */
  record ApplyCommand(
      String displayName, String slug, String category, String bio, String location) {}

  record UpdateCommand(String displayName, String bio, String location) {}

  /** 与 OpenAPI CreatorDetail 对齐的只读投影基础字段。 */
  record CreatorView(
      UUID id,
      UUID userId,
      String slug,
      String displayName,
      String category,
      String bio,
      String location) {}
}
