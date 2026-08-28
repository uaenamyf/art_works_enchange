package com.creatorhub.catalog.application.port.in;

import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import java.util.UUID;

/** Creator 撤回已发布数字资产的用例；撤回后下载授权由下游事件消费者处理。 */
public interface DigitalAssetRecallUseCase {

  DigitalAssetVersion recall(UUID ownerId, UUID versionId, String reason);
}
