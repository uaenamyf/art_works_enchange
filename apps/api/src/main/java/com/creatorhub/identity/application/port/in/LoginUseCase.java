package com.creatorhub.identity.application.port.in;

import java.util.UUID;

/** 校验邮箱密码凭据并签发新会话的输入端口。 */
public interface LoginUseCase {

  /** 执行登录；失败时不应暴露邮箱是否存在或账号是否禁用。 */
  LoginResult login(LoginCommand command);

  /** 来自 HTTP 边界的不可信登录凭据。 */
  record LoginCommand(String email, String password) {}

  /** 登录结果及最小用户概要。Refresh Token 仅供接口层写入 HttpOnly Cookie。 */
  record LoginResult(
      String accessToken,
      long expiresIn,
      String refreshToken,
      UUID userId,
      String displayName,
      String avatarUrl,
      String role) {}
}
