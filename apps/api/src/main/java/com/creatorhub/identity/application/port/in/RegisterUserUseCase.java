package com.creatorhub.identity.application.port.in;

import java.util.UUID;

/** 注册账号并创建首个认证会话的输入端口。 */
public interface RegisterUserUseCase {

  /** 执行注册；接口层负责协议校验，应用层仍需守住业务边界。 */
  RegistrationResult register(RegisterCommand command);

  /** 来自 HTTP 边界的不可信注册输入。 */
  record RegisterCommand(String email, String password, String displayName) {}

  /** 注册结果及最小用户概要。Refresh Token 只能由接口层转入安全 Cookie，不能进入响应 JSON。 */
  record RegistrationResult(
      String accessToken,
      long expiresIn,
      String refreshToken,
      UUID userId,
      String displayName,
      String avatarUrl,
      String role) {}
}
