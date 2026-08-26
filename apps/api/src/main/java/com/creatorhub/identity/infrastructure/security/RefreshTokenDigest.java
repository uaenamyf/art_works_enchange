package com.creatorhub.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Refresh Token 摘要工具，统一使用 SHA-256 + Base64URL 无填充编码。
 *
 * <p>数据库只保存摘要；明文 Token 仅存在于请求 Cookie 和签发结果的短生命周期内。
 */
public final class RefreshTokenDigest {

  private RefreshTokenDigest() {}

  /** 将不透明 Refresh Token 转为稳定数据库查询键。 */
  public static String digest(String token) {
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
