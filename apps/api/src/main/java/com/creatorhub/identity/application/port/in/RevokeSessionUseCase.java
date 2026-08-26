package com.creatorhub.identity.application.port.in;

/** 撤销当前 Refresh 会话的输入端口。 */
public interface RevokeSessionUseCase {

  /** 重复注销保持幂等，不向调用方暴露 Token 是否曾存在。 */
  void revoke(String refreshToken);
}
