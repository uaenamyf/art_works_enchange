package com.creatorhub.identity.application.port.in;

/** 使用 HttpOnly Cookie 中的 Refresh Token 轮换认证会话。 */
public interface RotateSessionUseCase {

  RotateResult rotate(String refreshToken);

  /** 轮换结果；新 Refresh Token 只能由接口层重新写入安全 Cookie。 */
  record RotateResult(
      String accessToken,
      long expiresIn,
      String refreshToken,
      java.util.UUID userId,
      String displayName,
      String avatarUrl,
      String role) {}
}
