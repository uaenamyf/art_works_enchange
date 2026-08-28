package com.creatorhub.digitaldelivery.application.port.out;

import java.util.UUID;

/** 数字下载授权限流端口；实现只能影响凭证签发频率，不能替代授权校验。 */
public interface DownloadRateLimiter {

  void check(UUID buyerId, String requestIp);
}
