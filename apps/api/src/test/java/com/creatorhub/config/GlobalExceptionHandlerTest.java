package com.creatorhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.sharedkernel.error.BusinessError;
import org.junit.jupiter.api.Test;

/** 验证新增数字交付和通知错误码映射到正确的 HTTP 语义。 */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void notificationNotFoundMapsTo404() {
    var response = handler.handleBusiness(new BusinessError("NOTIFICATION_NOT_FOUND", "missing"));

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void downloadRateLimiterUnavailableMapsTo503() {
    var response =
        handler.handleBusiness(
            new BusinessError(
                "DOWNLOAD_RATE_LIMITER_UNAVAILABLE", "Download rate limiter is unavailable"));

    assertThat(response.getStatusCode().value()).isEqualTo(503);
  }
}
