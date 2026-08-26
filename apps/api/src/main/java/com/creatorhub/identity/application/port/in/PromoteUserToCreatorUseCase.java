package com.creatorhub.identity.application.port.in;

import java.util.UUID;

/** Creator 上下文申请成功后调用的 Identity 公开输入端口。 */
public interface PromoteUserToCreatorUseCase {

  /** 幂等提升用户角色为 CREATOR；禁用或不存在用户必须拒绝。 */
  void promote(UUID userId);
}
