package com.creatorhub.digitaldelivery.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.creatorhub.digitaldelivery.application.port.out.DownloadGrantRepository;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证数字资产撤回会撤销授权并失效未使用下载凭证。 */
class DigitalAssetRecallEventHandlerTest {

  @Test
  void revokesEntitlementsAndDownloadGrantsByAssetVersion() {
    EntitlementRepository entitlements = mock(EntitlementRepository.class);
    DownloadGrantRepository grants = mock(DownloadGrantRepository.class);
    DigitalAssetRecallEventHandler handler =
        new DigitalAssetRecallEventHandler(entitlements, grants);
    UUID assetVersionId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-08-29T00:00:00Z");

    handler.handle(
        OutboxEvent.pending(
            UUID.randomUUID(),
            "DigitalAssetRecalled",
            1,
            "DigitalAsset",
            UUID.randomUUID(),
            1,
            occurredAt,
            UUID.randomUUID(),
            Map.of("assetVersionId", assetVersionId.toString(), "reasonCode", "content update")));

    verify(entitlements)
        .revokeAvailableByAssetVersion(eq(assetVersionId), eq("content update"), eq(occurredAt));
    verify(grants).revokeByAssetVersion(eq(assetVersionId), eq(occurredAt));
  }

  @Test
  void ignoresUnsupportedEventVersion() {
    EntitlementRepository entitlements = mock(EntitlementRepository.class);
    DownloadGrantRepository grants = mock(DownloadGrantRepository.class);
    DigitalAssetRecallEventHandler handler =
        new DigitalAssetRecallEventHandler(entitlements, grants);

    handler.handle(
        OutboxEvent.pending(
            UUID.randomUUID(),
            "DigitalAssetRecalled",
            2,
            "DigitalAsset",
            UUID.randomUUID(),
            1,
            Instant.parse("2026-08-29T00:00:00Z"),
            UUID.randomUUID(),
            Map.of()));

    verify(entitlements, never())
        .revokeAvailableByAssetVersion(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    verify(grants, never())
        .revokeByAssetVersion(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMissingReasonCodeBeforeRevoking() {
    EntitlementRepository entitlements = mock(EntitlementRepository.class);
    DownloadGrantRepository grants = mock(DownloadGrantRepository.class);
    DigitalAssetRecallEventHandler handler =
        new DigitalAssetRecallEventHandler(entitlements, grants);

    assertThatThrownBy(
            () ->
                handler.handle(
                    OutboxEvent.pending(
                        UUID.randomUUID(),
                        "DigitalAssetRecalled",
                        1,
                        "DigitalAsset",
                        UUID.randomUUID(),
                        1,
                        Instant.parse("2026-08-29T00:00:00Z"),
                        UUID.randomUUID(),
                        Map.of("assetVersionId", UUID.randomUUID().toString()))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Digital asset recall payload missing reasonCode");

    verify(entitlements, never())
        .revokeAvailableByAssetVersion(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    verify(grants, never())
        .revokeByAssetVersion(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
