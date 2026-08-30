package com.creatorhub.webhook.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 通用 Webhook HMAC 验签器；只处理原始请求体，不解析载荷也不暴露密钥。 */
public final class HmacWebhookVerifier {

  private static final String ALGORITHM = "HmacSHA256";
  private static final String PREFIX = "sha256=";

  private HmacWebhookVerifier() {}

  /** 使用常量时间比较校验 Provider 签名，支持 sha256=hex 和纯 hex 两种格式。 */
  public static void verify(byte[] rawPayload, String signature, String secret) {
    if (rawPayload == null) throw new IllegalArgumentException("rawPayload is required");
    if (signature == null || signature.isBlank()) {
      throw new IllegalArgumentException("signature is required");
    }
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("secret is required");
    }

    byte[] expected = sign(rawPayload, secret);
    String providedText = signature.trim().toLowerCase(java.util.Locale.ROOT);
    if (providedText.startsWith(PREFIX)) providedText = providedText.substring(PREFIX.length());
    final byte[] provided;
    try {
      provided = HexFormat.of().parseHex(providedText);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("signature is invalid", exception);
    }
    if (!MessageDigest.isEqual(expected, provided)) {
      throw new IllegalArgumentException("signature is invalid");
    }
  }

  /** 仅供测试和 Provider 适配器生成签名，返回值不包含原始密钥。 */
  static String signatureFor(byte[] rawPayload, String secret) {
    return PREFIX + HexFormat.of().formatHex(sign(rawPayload, secret));
  }

  private static byte[] sign(byte[] rawPayload, String secret) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      return mac.doFinal(rawPayload);
    } catch (java.security.InvalidKeyException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Webhook HMAC is unavailable", exception);
    }
  }
}
