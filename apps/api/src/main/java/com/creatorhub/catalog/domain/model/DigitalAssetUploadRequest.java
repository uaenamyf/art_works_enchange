package com.creatorhub.catalog.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** 数字资产上传请求；只接受允许的文件类型和大小，文件内容仍需后续扫描确认。 */
public record DigitalAssetUploadRequest(
    String fileName, String contentType, long sizeBytes, String sha256) {

  public static final long MAX_SIZE_BYTES = 512L * 1024 * 1024;
  private static final Set<String> ALLOWED_TYPES =
      Set.of(
          "application/pdf",
          "application/zip",
          "application/epub+zip",
          "audio/mpeg",
          "audio/wav",
          "video/mp4",
          "font/ttf",
          "font/otf",
          "application/octet-stream");

  public DigitalAssetUploadRequest {
    fileName = required(fileName, "fileName");
    if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
      throw new IllegalArgumentException("File name is invalid");
    }
    contentType = required(contentType, "contentType").toLowerCase(Locale.ROOT);
    if (!ALLOWED_TYPES.contains(contentType)) {
      throw new IllegalArgumentException("Digital asset content type is not allowed");
    }
    if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
      throw new IllegalArgumentException("Digital asset size is invalid");
    }
    sha256 = required(sha256, "sha256").toLowerCase(Locale.ROOT);
    if (!sha256.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("sha256 must be a hexadecimal digest");
    }
  }

  private static String required(String value, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty() || normalized.length() > 512) {
      throw new IllegalArgumentException(name + " is invalid");
    }
    return normalized;
  }
}
