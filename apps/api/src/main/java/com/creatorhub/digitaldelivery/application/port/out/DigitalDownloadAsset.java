package com.creatorhub.digitaldelivery.application.port.out;

import java.util.UUID;

/** 已通过授权校验的数字资产下载快照，不向接口层暴露数据库行或长期存储凭证。 */
public record DigitalDownloadAsset(
    UUID entitlementId,
    UUID assetVersionId,
    String fileName,
    String objectKey,
    String contentType,
    long sizeBytes,
    int version) {}
