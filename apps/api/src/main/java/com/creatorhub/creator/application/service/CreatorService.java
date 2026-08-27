package com.creatorhub.creator.application.service;

import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.creator.domain.model.Creator;
import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.creator.domain.repository.CreatorRepository;
import com.creatorhub.identity.application.port.in.PromoteUserToCreatorUseCase;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creator 应用服务，编排唯一性检查、聚合保存和 Identity 角色提升。
 *
 * <p>跨上下文只调用 Identity 公开输入端口，不访问 Identity Repository、Mapper 或 PersistenceRow。
 */
@Service
@Transactional
public class CreatorService implements CreatorUseCases {

  private final CreatorRepository creators;
  private final PromoteUserToCreatorUseCase promoteUser;

  public CreatorService(CreatorRepository creators, PromoteUserToCreatorUseCase promoteUser) {
    this.creators = creators;
    this.promoteUser = promoteUser;
  }

  @Override
  public CreatorView apply(UUID userId, ApplyCommand command) {
    UserId owner = UserId.of(userId);
    if (creators.existsByUserId(owner)) {
      throw new BusinessError("CREATOR_ALREADY_EXISTS", "User already has a creator profile");
    }
    if (creators.existsBySlug(command.slug())) {
      throw new BusinessError("CREATOR_SLUG_TAKEN", "Creator slug is already taken");
    }

    Creator creator =
        Creator.apply(
            CreatorId.random(),
            owner,
            command.slug(),
            command.displayName(),
            command.category(),
            command.bio(),
            command.location(),
            AppClock.now());
    Creator saved = creators.save(creator);
    // 通过 Identity 公开端口提升角色，避免 Creator 直接依赖用户仓储。
    promoteUser.promote(userId);
    return toView(saved);
  }

  @Override
  public CreatorView update(UUID userId, UpdateCommand command) {
    Creator creator =
        creators
            .findByUserId(UserId.of(userId))
            .orElseThrow(() -> new BusinessError("CREATOR_NOT_FOUND", "Creator profile not found"));
    creator.updateProfile(
        command.displayName(), creator.category(), command.bio(), command.location());
    return toView(creators.save(creator));
  }

  @Override
  @Transactional(readOnly = true)
  public CreatorView get(UUID creatorId) {
    return creators
        .findById(CreatorId.of(creatorId))
        .map(CreatorService::toView)
        .orElseThrow(() -> new BusinessError("CREATOR_NOT_FOUND", "Creator profile not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public CreatorView getByUserId(UUID userId) {
    return creators
        .findByUserId(UserId.of(userId))
        .map(CreatorService::toView)
        .orElseThrow(() -> new BusinessError("CREATOR_NOT_FOUND", "Creator profile not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public CreatorPage list(String category, String cursor, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decode(cursor);
    List<Creator> results =
        creators.findPage(
            new CreatorRepository.PageQuery(
                category == null || category.isBlank() ? null : category.trim(),
                decoded.createdAt(),
                decoded.id(),
                pageSize + 1));
    boolean hasMore = results.size() > pageSize;
    List<Creator> page = results.stream().limit(pageSize).toList();
    return new CreatorPage(
        page.stream()
            .map(
                creator ->
                    new CreatorSummaryView(
                        creator.id().value(),
                        creator.slug(),
                        creator.displayName(),
                        null,
                        creator.category(),
                        false,
                        0,
                        false))
            .toList(),
        hasMore ? Cursor.encode(page.get(page.size() - 1)) : null,
        hasMore);
  }

  private record Cursor(Instant createdAt, CreatorId id) {
    static Cursor decode(String value) {
      if (value == null || value.isBlank()) return new Cursor(null, null);
      try {
        String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", 2);
        return new Cursor(Instant.parse(parts[0]), CreatorId.of(UUID.fromString(parts[1])));
      } catch (RuntimeException ex) {
        throw new BusinessError("INVALID_CURSOR", "Cursor is invalid");
      }
    }

    static String encode(Creator creator) {
      String value = creator.createdAt() + "|" + creator.id().value();
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static CreatorView toView(Creator creator) {
    return new CreatorView(
        creator.id().value(),
        creator.userId().value(),
        creator.slug(),
        creator.displayName(),
        creator.category(),
        creator.bio(),
        creator.location());
  }
}
