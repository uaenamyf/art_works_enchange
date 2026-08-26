package com.creatorhub.identity.infrastructure.security;

import com.creatorhub.identity.application.port.out.SessionIssuer;
import com.creatorhub.identity.application.port.out.SessionManager;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.identity.infrastructure.persistence.SessionStore;
import com.creatorhub.identity.infrastructure.persistence.SessionStore.StoredSession;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * 持久化会话管理器，负责 Refresh Token 轮换和撤销。
 *
 * <p>轮换通过数据库悲观锁保证旧 Token 最多被消费一次；无效、过期、已撤销和用户不存在 统一返回 {@code REFRESH_TOKEN_INVALID}，不泄露会话内部状态。
 */
@Component
public class PersistentSessionManager implements SessionManager {

  private final SessionStore sessions;
  private final UserRepository users;
  private final SessionIssuer issuer;

  public PersistentSessionManager(
      SessionStore sessions, UserRepository users, SessionIssuer issuer) {
    this.sessions = sessions;
    this.users = users;
    this.issuer = issuer;
  }

  @Override
  public RotatedSession rotate(String refreshToken) {
    Instant now = AppClock.now();
    StoredSession current = findSession(refreshToken);
    if (current.revokedAt() != null || !current.expiresAt().isAfter(now)) {
      throw invalidRefreshToken();
    }

    // 先撤销旧会话，再签发新会话；外层事务保证两个动作原子提交。
    sessions.revoke(current.id(), now);
    var user =
        users
            .findById(UserId.of(current.userId()))
            .filter(com.creatorhub.identity.domain.model.User::canIssueSession)
            .orElseThrow(PersistentSessionManager::invalidRefreshToken);
    return new RotatedSession(issuer.issue(user), user);
  }

  @Override
  public void revoke(String refreshToken) {
    sessions
        .findForUpdate(RefreshTokenDigest.digest(refreshToken))
        .ifPresent(session -> sessions.revoke(session.id(), AppClock.now()));
  }

  private StoredSession findSession(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw invalidRefreshToken();
    }
    return sessions
        .findForUpdate(RefreshTokenDigest.digest(refreshToken))
        .orElseThrow(PersistentSessionManager::invalidRefreshToken);
  }

  private static BusinessError invalidRefreshToken() {
    return new BusinessError("REFRESH_TOKEN_INVALID", "Refresh token is invalid or expired");
  }
}
