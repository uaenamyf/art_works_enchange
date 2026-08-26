package com.creatorhub.creator.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.sharedkernel.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 验证 Creator 的 slug、公开资料长度以及 owner/slug 不可变语义。 */
class CreatorTest {

  @Test
  void createsAndUpdatesProfileWithoutChangingSlugOrOwner() {
    UserId userId = UserId.random();
    Creator creator =
        Creator.apply(
            CreatorId.random(),
            userId,
            "alice-studio",
            "Alice Studio",
            "ILLUSTRATION",
            "Initial bio",
            null,
            Instant.parse("2026-08-26T00:00:00Z"));

    creator.updateProfile("Alice Atelier", "PAINTING", "Updated bio", "Shanghai");

    assertThat(creator.userId()).isEqualTo(userId);
    assertThat(creator.slug()).isEqualTo("alice-studio");
    assertThat(creator.displayName()).isEqualTo("Alice Atelier");
  }

  @Test
  void rejectsInvalidSlugAndOversizedBio() {
    assertThatThrownBy(
            () ->
                Creator.apply(
                    CreatorId.random(),
                    UserId.random(),
                    "Invalid Slug",
                    "Alice Studio",
                    "ILLUSTRATION",
                    "Bio",
                    null,
                    Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("slug");

    Creator creator =
        Creator.apply(
            CreatorId.random(),
            UserId.random(),
            "alice-studio",
            "Alice Studio",
            "ILLUSTRATION",
            "Bio",
            null,
            Instant.now());
    assertThatThrownBy(
            () -> creator.updateProfile("Alice Studio", "ILLUSTRATION", "x".repeat(2001), null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
