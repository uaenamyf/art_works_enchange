package com.creatorhub.digitaldelivery.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.creatorhub.digitaldelivery.domain.model.Entitlement;
import com.creatorhub.digitaldelivery.domain.repository.EntitlementRepository;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证支付成功事件只产生 AVAILABLE 授权，重复事件由仓储幂等返回已有授权。 */
class DigitalEntitlementServiceTest {

  @Test
  void grantsAvailableEntitlementAfterTrustedPaymentEvent() {
    EntitlementRepository repository = mock(EntitlementRepository.class);
    Entitlement stored = entitlement();
    when(repository.grant(any(Entitlement.class))).thenReturn(stored);

    Entitlement result =
        new DigitalEntitlementService(repository)
            .grantAfterPayment(
                UUID.randomUUID(),
                stored.orderId(),
                stored.buyerId(),
                stored.artworkId(),
                stored.assetVersionId(),
                Instant.parse("2026-08-28T01:00:00Z"));

    assertThat(result.status())
        .isEqualTo(com.creatorhub.digitaldelivery.domain.model.EntitlementStatus.AVAILABLE);
  }

  @Test
  void repositoryReplayReturnsExistingEntitlement() {
    EntitlementRepository repository = mock(EntitlementRepository.class);
    Entitlement existing = entitlement();
    when(repository.grant(any(Entitlement.class))).thenReturn(existing);

    Entitlement result =
        new DigitalEntitlementService(repository)
            .grantAfterPayment(
                UUID.randomUUID(),
                existing.orderId(),
                existing.buyerId(),
                existing.artworkId(),
                existing.assetVersionId(),
                Instant.now());

    assertThat(result.id()).isEqualTo(existing.id());
  }

  @Test
  void nonBuyerCannotReadEntitlementAndGetsNotFound() {
    EntitlementRepository repository = mock(EntitlementRepository.class);
    Entitlement existing = entitlement();
    UUID anotherBuyerId = UUID.randomUUID();
    when(repository.findByOrderId(existing.orderId())).thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                new DigitalEntitlementService(repository)
                    .getForBuyer(anotherBuyerId, existing.orderId()))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("DIGITAL_ENTITLEMENT_NOT_FOUND");
  }

  @Test
  void appendsGrantedEventWithoutDownloadSecrets() {
    EntitlementRepository repository = mock(EntitlementRepository.class);
    OutboxRepository outbox = mock(OutboxRepository.class);
    Entitlement stored = entitlement();
    UUID paymentEventId = UUID.randomUUID();
    when(repository.grant(any(Entitlement.class))).thenReturn(stored);

    new DigitalEntitlementService(repository, outbox)
        .grantAfterPayment(
            paymentEventId,
            stored.orderId(),
            stored.buyerId(),
            stored.artworkId(),
            stored.assetVersionId(),
            Instant.parse("2026-08-28T01:00:00Z"));

    var event = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
    org.mockito.Mockito.verify(outbox).append(event.capture());
    assertThat(event.getValue().eventType()).isEqualTo("DigitalEntitlementGranted");
    assertThat(event.getValue().traceId()).isEqualTo(paymentEventId);
    assertThat(event.getValue().payload())
        .doesNotContainKeys("objectKey", "downloadUrl", "downloadToken");
  }

  @Test
  void replaysTrustedPaymentWithSameOutboxEventId() {
    EntitlementRepository repository = mock(EntitlementRepository.class);
    OutboxRepository outbox = mock(OutboxRepository.class);
    Entitlement stored = entitlement();
    UUID paymentEventId = UUID.randomUUID();
    when(repository.grant(any(Entitlement.class))).thenReturn(stored);
    DigitalEntitlementService service = new DigitalEntitlementService(repository, outbox);

    service.grantAfterPayment(
        paymentEventId,
        stored.orderId(),
        stored.buyerId(),
        stored.artworkId(),
        stored.assetVersionId(),
        Instant.parse("2026-08-28T01:00:00Z"));
    service.grantAfterPayment(
        paymentEventId,
        stored.orderId(),
        stored.buyerId(),
        stored.artworkId(),
        stored.assetVersionId(),
        Instant.parse("2026-08-28T01:00:00Z"));

    var events =
        org.mockito.Mockito.mockingDetails(outbox).getInvocations().stream()
            .map(invocation -> (OutboxEvent) invocation.getArguments()[0])
            .toList();
    assertThat(events).hasSize(2);
    assertThat(events.get(0).eventId()).isEqualTo(events.get(1).eventId());
  }

  private static Entitlement entitlement() {
    return Entitlement.available(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-08-28T00:00:00Z"));
  }
}
