package com.creatorhub.webhook.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 通用 Webhook HMAC 验签器；只处理原始请求体，不解析载荷也不暴露密钥。 */
public final class HmacWebhookVerifier {

  private static final String ALGORITHM = "HmacSHA256";
  private static final String PREFIX = "sha256=";
  private static final Pattern TIMESTAMPED_SIGNATURE =
      Pattern.compile("^t=(\\d+),v1=([0-9a-fA-F]+)$");

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

  /** 校验带时间戳的签名并限制有效窗口，防止合法 Webhook 被截获后重放。 */
  public static void verifyTimestamped(
      byte[] rawPayload, String signature, String secret, Instant now, Duration tolerance) {
    if (now == null) throw new IllegalArgumentException("now is required");
    if (tolerance == null || tolerance.isNegative()) {
      throw new IllegalArgumentException("tolerance is invalid");
    }
    if (rawPayload == null) throw new IllegalArgumentException("rawPayload is required");
    if (signature == null || signature.isBlank()) {
      throw new IllegalArgumentException("signature is required");
    }
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("secret is required");
    }
    Matcher matcher = TIMESTAMPED_SIGNATURE.matcher(signature.trim());
    if (!matcher.matches()) throw new IllegalArgumentException("signature is invalid");

    final long timestamp;
    try {
      timestamp = Long.parseLong(matcher.group(1));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("signature is invalid", exception);
    }
    Instant signedAt;
    try {
      signedAt = Instant.ofEpochSecond(timestamp);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("signature is invalid", exception);
    }
    if (Duration.between(signedAt, now).abs().compareTo(tolerance) > 0) {
      throw new IllegalArgumentException("signature is expired");
    }

    byte[] signedPayload =
        new byte
            [Long.toString(timestamp).getBytes(StandardCharsets.UTF_8).length
                + 1
                + rawPayload.length];
    byte[] timestampBytes = Long.toString(timestamp).getBytes(StandardCharsets.UTF_8);
    System.arraycopy(timestampBytes, 0, signedPayload, 0, timestampBytes.length);
    signedPayload[timestampBytes.length] = '.';
    System.arraycopy(rawPayload, 0, signedPayload, timestampBytes.length + 1, rawPayload.length);
    byte[] provided;
    try {
      provided = HexFormat.of().parseHex(matcher.group(2).toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("signature is invalid", exception);
    }
    if (!MessageDigest.isEqual(sign(signedPayload, secret), provided)) {
      throw new IllegalArgumentException("signature is invalid");
    }
  }

  /** 仅供测试和 Provider 适配器生成签名，返回值不包含原始密钥。 */
  static String signatureFor(byte[] rawPayload, String secret) {
    return PREFIX + HexFormat.of().formatHex(sign(rawPayload, secret));
  }

  static String timestampedSignatureFor(byte[] rawPayload, String secret, long timestamp) {
    byte[] timestampBytes = Long.toString(timestamp).getBytes(StandardCharsets.UTF_8);
    byte[] signedPayload = new byte[timestampBytes.length + 1 + rawPayload.length];
    System.arraycopy(timestampBytes, 0, signedPayload, 0, timestampBytes.length);
    signedPayload[timestampBytes.length] = '.';
    System.arraycopy(rawPayload, 0, signedPayload, timestampBytes.length + 1, rawPayload.length);
    return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(sign(signedPayload, secret));
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
