package com.creatorhub.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** 验证媒体预签名请求的文件类型、大小和对象 key 输入约束。 */
class MediaUploadRequestTest {

  @Test
  void acceptsAllowedArtworkImage() {
    assertDoesNotThrow(
        () ->
            new MediaUploadRequest(
                "cover.webp", "image/webp", 1024, MediaUploadRequest.Purpose.ARTWORK));
  }

  @Test
  void rejectsUnsupportedType() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MediaUploadRequest(
                "script.svg", "image/svg+xml", 1024, MediaUploadRequest.Purpose.ARTWORK));
  }

  @Test
  void rejectsPathTraversalFileName() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MediaUploadRequest(
                "../private.txt", "image/png", 1024, MediaUploadRequest.Purpose.ARTWORK));
  }

  @Test
  void rejectsOversizedFile() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MediaUploadRequest(
                "large.png",
                "image/png",
                MediaUploadRequest.MAX_SIZE_BYTES + 1,
                MediaUploadRequest.Purpose.ARTWORK));
  }
}
