package com.creatorhub.identity.application.port.out;

/** 管理 Refresh 会话轮换与撤销的输出端口。 */
public interface SessionManager {

  /** 消费当前 Refresh Token 并签发新会话；旧 Token 必须在同一事务中立即失效。 */
  RotatedSession rotate(String refreshToken);

  /** 撤销当前 Refresh Token；重复撤销保持幂等。 */
  void revoke(String refreshToken);

  /** 轮换后的新会话及用户最小身份快照。 */
  record RotatedSession(
      SessionIssuer.IssuedSession session, com.creatorhub.identity.domain.model.User user) {}
}
