package com.creatorhub.creator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.creator.application.port.in.CreatorUseCases.ApplyCommand;
import com.creatorhub.creator.domain.model.Creator;
import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.creator.domain.repository.CreatorRepository;
import com.creatorhub.identity.application.port.in.PromoteUserToCreatorUseCase;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 Creator 申请唯一性、Identity 公开端口协作和当前用户 owner 约束。 */
class CreatorServiceTest {

  @Test
  void appliesCreatorAndPromotesIdentityRoleThroughPublicPort() {
    InMemoryCreators creators = new InMemoryCreators();
    RecordingPromotion promotion = new RecordingPromotion();
    CreatorService service = new CreatorService(creators, promotion, followStats());
    UUID userId = UUID.randomUUID();

    var result =
        service.apply(
            userId,
            new ApplyCommand("Alice Studio", "alice-studio", "ILLUSTRATION", "Bio", "Shanghai"));

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.slug()).isEqualTo("alice-studio");
    assertThat(promotion.promoted).isEqualTo(userId);
  }

  @Test
  void rejectsSecondCreatorForSameUser() {
    InMemoryCreators creators = new InMemoryCreators();
    CreatorService service = new CreatorService(creators, userId -> {}, followStats());
    UUID userId = UUID.randomUUID();
    ApplyCommand command =
        new ApplyCommand("Alice Studio", "alice-studio", "ILLUSTRATION", "Bio", null);
    service.apply(userId, command);

    assertThatThrownBy(
            () ->
                service.apply(
                    userId,
                    new ApplyCommand("Second Studio", "second-studio", "PAINTING", "Bio", null)))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("CREATOR_ALREADY_EXISTS");
  }

  @Test
  void updatesPayPalMerchantAccountForCurrentCreator() {
    InMemoryCreators creators = new InMemoryCreators();
    CreatorService service = new CreatorService(creators, userId -> {}, followStats());
    UUID userId = UUID.randomUUID();
    service.apply(
        userId, new ApplyCommand("Alice Studio", "alice-studio", "ILLUSTRATION", "Bio", null));

    var result = service.updatePayPalAccount(userId, "merchant-123");

    assertThat(result).isEqualTo(new CreatorUseCases.PayPalAccountView(true, "merchant-123"));
    assertThat(creators.findByUserId(UserId.of(userId)).orElseThrow().payPalMerchantId())
        .isEqualTo("merchant-123");
  }

  @Test
  void rejectsInvalidPayPalMerchantAccount() {
    InMemoryCreators creators = new InMemoryCreators();
    CreatorService service = new CreatorService(creators, userId -> {}, followStats());
    UUID userId = UUID.randomUUID();
    service.apply(
        userId, new ApplyCommand("Alice Studio", "alice-studio", "ILLUSTRATION", "Bio", null));

    assertThatThrownBy(() -> service.updatePayPalAccount(userId, "paypal@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("PayPal merchantId is invalid");
  }

  private static final class RecordingPromotion implements PromoteUserToCreatorUseCase {
    UUID promoted;

    public void promote(UUID userId) {
      promoted = userId;
    }
  }

  private static com.creatorhub.follow.application.port.in.FollowUseCases.ReadPort followStats() {
    return new com.creatorhub.follow.application.port.in.FollowUseCases.ReadPort() {
      public long countByCreatorId(UUID creatorId) {
        return 0;
      }

      public boolean isFollowing(UUID userId, UUID creatorId) {
        return false;
      }
    };
  }

  private static final class InMemoryCreators implements CreatorRepository {
    private final List<Creator> creators = new ArrayList<>();

    public boolean existsByUserId(UserId userId) {
      return creators.stream().anyMatch(creator -> creator.userId().equals(userId));
    }

    public boolean existsBySlug(String slug) {
      return creators.stream().anyMatch(creator -> creator.slug().equals(slug));
    }

    public Optional<Creator> findById(CreatorId id) {
      return creators.stream().filter(creator -> creator.id().equals(id)).findFirst();
    }

    public Optional<Creator> findByUserId(UserId userId) {
      return creators.stream().filter(creator -> creator.userId().equals(userId)).findFirst();
    }

    public List<Creator> findPage(PageQuery query) {
      return List.of();
    }

    public Creator save(Creator creator) {
      creators.removeIf(existing -> existing.id().equals(creator.id()));
      creators.add(creator);
      return creator;
    }
  }
}
