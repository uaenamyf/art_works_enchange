package com.creatorhub.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 会话持久化适配器，供会话签发器保存 Refresh 会话。
 *
 * <p>{@code refreshTokenHash} 必须是不可逆摘要；调用方不得传入或记录明文 Refresh Token。
 */
@Component
public class SessionStore {

  private final SessionMapper mapper;

  public SessionStore(SessionMapper mapper) {
    this.mapper = mapper;
  }

  public void create(
      UUID id, UUID userId, String refreshTokenHash, Instant expiresAt, Instant createdAt) {
    // 这里只接受摘要，明文生命周期在 JwtSessionIssuer.issue 返回后即结束。
    SessionPersistenceRow row = new SessionPersistenceRow();
    row.setId(id);
    row.setUserId(userId);
    row.setRefreshTokenHash(refreshTokenHash);
    row.setExpiresAt(expiresAt);
    row.setCreatedAt(createdAt);
    mapper.insert(row);
  }

  /** 按摘要悲观锁定会话，供轮换用例原子消费。 */
  public Optional<StoredSession> findForUpdate(String refreshTokenHash) {
    return Optional.ofNullable(mapper.findByRefreshTokenHashForUpdate(refreshTokenHash))
        .map(SessionStore::toStoredSession);
  }

  /** 幂等撤销已锁定会话；已撤销会话保持原时间。 */
  public void revoke(UUID sessionId, Instant revokedAt) {
    SessionPersistenceRow row = mapper.findByIdValue(sessionId);
    if (row != null && row.getRevokedAt() == null) {
      row.setRevokedAt(revokedAt);
      mapper.updateById(row);
    }
  }

  private static StoredSession toStoredSession(SessionPersistenceRow row) {
    return new StoredSession(row.getId(), row.getUserId(), row.getExpiresAt(), row.getRevokedAt());
  }

  /** 会话轮换所需的最小持久化快照，不包含 Token 明文或摘要。 */
  public record StoredSession(UUID id, UUID userId, Instant expiresAt, Instant revokedAt) {}
}
