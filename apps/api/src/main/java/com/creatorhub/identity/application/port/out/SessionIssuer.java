package com.creatorhub.identity.application.port.out;

import com.creatorhub.identity.domain.model.User;

/** 为已经完成身份资格校验的用户签发访问令牌与不透明刷新令牌。 */
public interface SessionIssuer {

  /** 签发新会话；调用方必须先校验账号未禁用且凭据有效。 */
  IssuedSession issue(User user);

  /** 会话签发结果。Refresh Token 仅供接口层写入 HttpOnly Cookie，禁止序列化到响应体。 */
  record IssuedSession(String accessToken, long expiresIn, String refreshToken) {}
}
