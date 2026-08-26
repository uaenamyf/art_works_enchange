package com.creatorhub.catalog.infrastructure.persistence;

import java.util.UUID;

/** 公开媒体查询的基础设施投影，不把存储对象 key 透传到 HTTP 层。 */
record MediaPublicPersistenceRow(UUID id, String objectKey, String contentType) {}
