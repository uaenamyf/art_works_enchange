package com.creatorhub.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证作品审核门禁和生命周期状态机，确保发布不能绕过审核。 */
class ArtworkTest {

  private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

  @Test
  void cannotPublishBeforeApproval() {
    Artwork artwork = draft(List.of(UUID.randomUUID()));

    assertThrows(IllegalStateException.class, () -> artwork.publish(NOW));
    assertEquals(ArtworkStatus.DRAFT, artwork.status());
  }

  @Test
  void reviewApprovalIsRequiredBeforePublishing() {
    Artwork artwork = draft(List.of(UUID.randomUUID()));

    artwork.submitForReview(NOW);
    assertEquals(ArtworkStatus.PENDING_REVIEW, artwork.status());
    artwork.approve(NOW);
    artwork.publish(NOW);

    assertEquals(ArtworkStatus.PUBLISHED, artwork.status());
  }

  @Test
  void rejectedArtworkMustReturnToDraftBeforeResubmission() {
    Artwork artwork = draft(List.of(UUID.randomUUID()));

    artwork.submitForReview(NOW);
    artwork.reject(NOW);
    assertEquals(ArtworkStatus.REJECTED, artwork.status());

    artwork.update(
        "Updated artwork",
        "A sufficiently long description for the updated artwork.",
        "ART",
        Money.of("99.00", "USD"),
        2,
        List.of(UUID.randomUUID()),
        NOW);

    assertEquals(ArtworkStatus.DRAFT, artwork.status());
    assertThrows(IllegalStateException.class, () -> artwork.publish(NOW));
  }

  @Test
  void emptyMediaCannotEnterReview() {
    Artwork artwork = draft(List.of());

    assertThrows(IllegalStateException.class, () -> artwork.submitForReview(NOW));
    assertEquals(ArtworkStatus.DRAFT, artwork.status());
  }

  @Test
  void blockedArtworkCannotBePublishedAgain() {
    Artwork artwork = draft(List.of(UUID.randomUUID()));

    artwork.submitForReview(NOW);
    artwork.approve(NOW);
    artwork.publish(NOW);
    artwork.block(NOW);

    assertEquals(ArtworkStatus.BLOCKED, artwork.status());
    assertThrows(IllegalStateException.class, () -> artwork.publish(NOW));
  }

  private static Artwork draft(List<UUID> mediaIds) {
    return Artwork.draft(
        ArtworkId.random(),
        UserId.random(),
        UUID.randomUUID(),
        "Test artwork",
        "A sufficiently long description for the test artwork.",
        "ART",
        Money.of("99.00", "USD"),
        2,
        mediaIds,
        NOW);
  }
}
