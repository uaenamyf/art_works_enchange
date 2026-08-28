package com.creatorhub.digitaldelivery.application.port.out;

import java.time.Instant;

/** 数字资产私有下载端口；只在授权校验后生成短期地址，不暴露 object key。 */
public interface DigitalAssetPrivateStoragePort {

  String presignDownload(String objectKey, String contentType, Instant expiresAt);
}
