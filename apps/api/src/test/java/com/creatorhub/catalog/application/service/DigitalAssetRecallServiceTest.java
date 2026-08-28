package com.creatorhub.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.application.port.out.DigitalAssetStore;
import com.creatorhub.catalog.domain.model.DigitalAssetScanStatus;
import com.creatorhub.catalog.domain.model.DigitalAssetVersion;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 验证数字资产撤回使用 owner 归属和 READY 版本，并发布脱敏撤回事件。 */
class DigitalAssetRecallServiceTest {

  @Test
  void recallsOwnedReadyVersionAndPublishesEvent() {
    DigitalAssetStore assets = mock(DigitalAssetStore.class);
    OutboxRepository outbox = mock(OutboxRepository.class);
    UUID ownerId = UUID.randomUUID();
    UUID artworkId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    DigitalAssetVersion recalled = version(versionId, DigitalAssetScanStatus.READY);
    when(assets.findArtworkId(ownerId, versionId)).thenReturn(artworkId);
    when(assets.recall(any(), any(), any(), any())).thenReturn(recalled);

    DigitalAssetVersion result =
        new DigitalAssetRecallService(assets, outbox).recall(ownerId, versionId, "版权调整");

    assertThat(result.id()).isEqualTo(versionId);
    ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outbox).append(event.capture());
    assertThat(event.getValue().eventType()).isEqualTo("DigitalAssetRecalled");
    assertThat(event.getValue().payload())
        .containsEntry("artworkId", artworkId.toString())
        .containsEntry("recipientId", ownerId.toString())
        .containsEntry("reasonCode", "版权调整")
        .doesNotContainKeys("objectKey", "downloadUrl", "downloadToken");
  }

  private static DigitalAssetVersion version(UUID id, DigitalAssetScanStatus status) {
    return new DigitalAssetVersion(
        id,
        UUID.randomUUID(),
        1,
        "asset.zip",
        "private/digital/asset.zip",
        "application/zip",
        1024,
        "a".repeat(64),
        status,
        null,
        Instant.parse("2026-08-28T00:00:00Z"),
        Instant.parse("2026-08-28T00:01:00Z"));
  }
}
