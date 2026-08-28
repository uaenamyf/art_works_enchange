package com.creatorhub.digitaldelivery.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.digitaldelivery.application.port.out.DigitalAssetPrivateStoragePort;
import com.creatorhub.digitaldelivery.application.port.out.DigitalDownloadAsset;
import com.creatorhub.digitaldelivery.application.port.out.DownloadGrantRepository;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 验证下载地址只能由买家有效授权触发，并且返回短期 URL。 */
class DigitalDownloadServiceTest {

  @Test
  void authorizedBuyerReceivesShortLivedDownloadUrl() {
    EntitlementRepository entitlements = mock(EntitlementRepository.class);
    DigitalAssetPrivateStoragePort storage = mock(DigitalAssetPrivateStoragePort.class);
    DownloadGrantRepository grants = mock(DownloadGrantRepository.class);
    UUID buyerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    DigitalDownloadAsset asset =
        new DigitalDownloadAsset(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "bundle.zip",
            "private/digital/bundle",
            "application/zip",
            2048,
            2);
    when(entitlements.findDownloadAsset(buyerId, orderId)).thenReturn(Optional.of(asset));
    when(storage.presignDownload(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn("https://private.example/download");

    DigitalDownloadService.DownloadView view =
        new DigitalDownloadService(entitlements, storage, grants, 300)
            .createGrant(buyerId, orderId);

    assertThat(view.downloadUrl()).isEqualTo("https://private.example/download");
    assertThat(view.fileName()).isEqualTo("bundle.zip");
    assertThat(view.expiresAt()).isAfter(Instant.now());
  }

  @Test
  void unauthorizedBuyerCannotReceiveDownloadUrl() {
    EntitlementRepository entitlements = mock(EntitlementRepository.class);
    DigitalAssetPrivateStoragePort storage = mock(DigitalAssetPrivateStoragePort.class);
    DownloadGrantRepository grants = mock(DownloadGrantRepository.class);
    when(entitlements.findDownloadAsset(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new DigitalDownloadService(entitlements, storage, grants, 300)
                    .createGrant(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("DIGITAL_DOWNLOAD_NOT_AVAILABLE");
  }

  @Test
  void storesHashedRequestContextInDownloadGrant() {
    EntitlementRepository entitlements = mock(EntitlementRepository.class);
    DigitalAssetPrivateStoragePort storage = mock(DigitalAssetPrivateStoragePort.class);
    DownloadGrantRepository grants = mock(DownloadGrantRepository.class);
    UUID buyerId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    when(entitlements.findDownloadAsset(buyerId, orderId))
        .thenReturn(
            Optional.of(
                new DigitalDownloadAsset(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "bundle.zip",
                    "private/bundle",
                    "application/zip",
                    10,
                    1)));
    when(storage.presignDownload(anyString(), anyString(), any(Instant.class)))
        .thenReturn("https://private.example/download");

    new DigitalDownloadService(entitlements, storage, grants, 300)
        .createGrant(buyerId, orderId, "203.0.113.10", "CreatorHubTest/1.0");

    ArgumentCaptor<com.creatorhub.digitaldelivery.domain.model.DownloadGrant> grant =
        ArgumentCaptor.forClass(com.creatorhub.digitaldelivery.domain.model.DownloadGrant.class);
    verify(grants).save(grant.capture());
    assertThat(grant.getValue().requestIpHash()).hasSize(64).doesNotContain("203.0.113.10");
    assertThat(grant.getValue().userAgentHash()).hasSize(64).doesNotContain("CreatorHubTest");
  }
}
