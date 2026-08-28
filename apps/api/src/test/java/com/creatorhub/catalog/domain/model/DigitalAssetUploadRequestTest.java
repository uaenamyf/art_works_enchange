package com.creatorhub.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 验证数字资产上传的文件类型、文件名、大小和 SHA-256 约束。 */
class DigitalAssetUploadRequestTest {

  @Test
  void acceptsSupportedPrivateFile() {
    DigitalAssetUploadRequest request =
        new DigitalAssetUploadRequest("bundle.zip", "APPLICATION/ZIP", 1024, "A".repeat(64));

    assertThat(request.contentType()).isEqualTo("application/zip");
    assertThat(request.sha256()).isEqualTo("a".repeat(64));
  }

  @Test
  void rejectsExecutableContentType() {
    assertThatThrownBy(
            () ->
                new DigitalAssetUploadRequest(
                    "bundle.exe", "application/x-msdownload", 1024, "a".repeat(64)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Digital asset content type is not allowed");
  }

  @Test
  void rejectsPathTraversalFileName() {
    assertThatThrownBy(
            () ->
                new DigitalAssetUploadRequest(
                    "../bundle.zip", "application/zip", 1024, "a".repeat(64)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File name is invalid");
  }
}
