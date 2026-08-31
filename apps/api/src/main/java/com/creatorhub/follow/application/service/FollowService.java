package com.creatorhub.follow.application.service;

import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.creator.application.port.in.CreatorUseCases.CreatorSummaryView;
import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.follow.application.port.in.FollowUseCases;
import com.creatorhub.follow.domain.repository.FollowRepository;
import com.creatorhub.identity.application.port.in.GetCurrentUserUseCase;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import com.creatorhub.outbox.domain.repository.OutboxRepository;
import com.creatorhub.sharedkernel.UserId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 关注应用服务；先确认 Creator 存在，再执行幂等关系变更并返回服务端计数。 */
@Service
@Transactional
public class FollowService implements FollowUseCases {

  private final FollowRepository follows;
  private final CreatorUseCases creators;
  private final GetCurrentUserUseCase users;
  private final OutboxRepository outbox;

  public FollowService(
      FollowRepository follows, CreatorUseCases creators, GetCurrentUserUseCase users) {
    this(follows, creators, users, null);
  }

  @Autowired
  public FollowService(
      FollowRepository follows,
      CreatorUseCases creators,
      GetCurrentUserUseCase users,
      OutboxRepository outbox) {
    this.follows = follows;
    this.creators = creators;
    this.users = users;
    this.outbox = outbox;
  }

  @Override
  public FollowState follow(UUID userId, UUID creatorId) {
    CreatorId id = CreatorId.of(creatorId);
    var creator = creators.get(creatorId);
    boolean added = follows.add(UserId.of(userId), id);
    if (added) publishFollowEvent(userId, creator);
    return new FollowState(true, follows.countByCreatorId(id));
  }

  @Override
  public FollowState unfollow(UUID userId, UUID creatorId) {
    CreatorId id = CreatorId.of(creatorId);
    creators.get(creatorId);
    follows.remove(UserId.of(userId), id);
    return new FollowState(false, follows.countByCreatorId(id));
  }

  @Override
  @Transactional(readOnly = true)
  public CreatorPage listMyFollows(UUID userId, String cursor, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decodeCreator(cursor);
    List<FollowRepository.FollowEntry> entries =
        follows.findByUser(
            UserId.of(userId), decoded.createdAt(), decoded.creatorId(), pageSize + 1);
    boolean hasMore = entries.size() > pageSize;
    List<FollowRepository.FollowEntry> page = entries.stream().limit(pageSize).toList();
    List<CreatorSummaryView> items =
        page.stream().map(entry -> summary(entry.creatorId().value(), userId)).toList();
    return new CreatorPage(
        items, hasMore ? Cursor.encode(page.get(page.size() - 1)) : null, hasMore);
  }

  @Override
  @Transactional(readOnly = true)
  public FollowerPage listFollowers(UUID creatorId, String cursor, int limit) {
    creators.get(creatorId);
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decodeUser(cursor);
    List<FollowRepository.FollowerEntry> entries =
        follows.findFollowers(
            CreatorId.of(creatorId), decoded.createdAt(), decoded.userId(), pageSize + 1);
    boolean hasMore = entries.size() > pageSize;
    List<FollowRepository.FollowerEntry> page = entries.stream().limit(pageSize).toList();
    List<com.creatorhub.identity.application.port.in.GetCurrentUserUseCase.UserSummary> items =
        page.stream().map(entry -> users.get(entry.userId().value())).toList();
    return new FollowerPage(
        items, hasMore ? Cursor.encode(page.get(page.size() - 1)) : null, hasMore);
  }

  private CreatorSummaryView summary(UUID creatorId, UUID userId) {
    var creator = creators.get(creatorId);
    return new CreatorSummaryView(
        creator.id(),
        creator.slug(),
        creator.displayName(),
        null,
        creator.category(),
        false,
        follows.countByCreatorId(CreatorId.of(creatorId)),
        follows.exists(UserId.of(userId), CreatorId.of(creatorId)));
  }

  private void publishFollowEvent(UUID followerId, CreatorUseCases.CreatorView creator) {
    if (outbox == null || creator.userId().equals(followerId)) return;
    outbox.append(
        OutboxEvent.pending(
            UUID.randomUUID(),
            "CreatorFollowed",
            1,
            "Creator",
            creator.id(),
            0,
            Instant.now(),
            UUID.randomUUID(),
            java.util.Map.of(
                "recipientId",
                creator.userId().toString(),
                "creatorId",
                creator.id().toString(),
                "title",
                "你有新的关注者",
                "body",
                "有人关注了你的 Creator 主页。")));
  }

  private record Cursor(Instant createdAt, CreatorId creatorId, UserId userId) {
    static Cursor decodeCreator(String value) {
      return decode(value, true);
    }

    static Cursor decodeUser(String value) {
      return decode(value, false);
    }

    private static Cursor decode(String value, boolean creator) {
      if (value == null || value.isBlank()) return new Cursor(null, null, null);
      try {
        String[] parts =
            new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                .split("\\|", 2);
        return creator
            ? new Cursor(Instant.parse(parts[0]), CreatorId.of(UUID.fromString(parts[1])), null)
            : new Cursor(Instant.parse(parts[0]), null, UserId.of(UUID.fromString(parts[1])));
      } catch (RuntimeException ex) {
        throw new com.creatorhub.sharedkernel.error.BusinessError(
            "INVALID_CURSOR", "Cursor is invalid");
      }
    }

    static String encode(FollowRepository.FollowEntry entry) {
      return encode(entry.createdAt(), entry.creatorId().value());
    }

    static String encode(FollowRepository.FollowerEntry entry) {
      return encode(entry.createdAt(), entry.userId().value());
    }

    private static String encode(Instant createdAt, UUID id) {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString((createdAt + "|" + id).getBytes(StandardCharsets.UTF_8));
    }
  }
}
