package com.creatorhub.catalog.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 媒体上传请求值对象，集中维护预签名上传前的安全约束。
 *
 * <p>只接受图片白名单和 15 MiB 以内的文件；客户端扩展名不能替代对象存储侧的文件头识别和病毒扫描。
 */
public record MediaUploadRequest(
    String fileName, String contentType, long sizeBytes, Purpose purpose) {

  public static final long MAX_SIZE_BYTES = 15 * 1024 * 1024;
  private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

  public MediaUploadRequest {
    fileName = requireFileName(fileName);
    contentType = requireContentType(contentType);
    if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
      throw new IllegalArgumentException("Media size is invalid");
    }
    purpose = Objects.requireNonNull(purpose, "purpose");
  }

  /** 上传用途决定后续对象归属和可绑定的业务资源范围。 */
  public enum Purpose {
    ARTWORK,
    AVATAR,
    CREATOR_COVER
  }

  private static String requireFileName(String value) {
    String normalized = Objects.requireNonNull(value, "fileName").trim();
    if (normalized.isEmpty()
        || normalized.length() > 255
        || normalized.contains("..")
        || normalized.contains("/")
        || normalized.contains("\\")) {
      throw new IllegalArgumentException("File name is invalid");
    }
    return normalized;
  }

  private static String requireContentType(String value) {
    String normalized =
        Objects.requireNonNull(value, "contentType").trim().toLowerCase(Locale.ROOT);
    if (!ALLOWED_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("Content type is not allowed");
    }
    return normalized;
  }
}
