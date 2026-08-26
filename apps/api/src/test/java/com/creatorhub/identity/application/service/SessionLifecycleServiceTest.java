package com.creatorhub.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatorhub.identity.application.port.out.SessionIssuer;
import com.creatorhub.identity.application.port.out.SessionManager;
import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.PasswordHash;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.sharedkernel.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 验证会话轮换/注销应用服务只编排端口调用，不泄漏基础设施细节。 */
class SessionLifecycleServiceTest {

  @Test
  void rotatesAndRevokesThroughSessionManager() {
    RecordingSessions sessions = new RecordingSessions();

    var rotated = new RotateSessionService(sessions).rotate("old-refresh");
    new RevokeSessionService(sessions).revoke("new-refresh");

    assertThat(rotated.accessToken()).isEqualTo("new-access");
    assertThat(rotated.refreshToken()).isEqualTo("new-refresh");
    assertThat(sessions.rotated).isEqualTo("old-refresh");
    assertThat(sessions.revoked).isEqualTo("new-refresh");
  }

  private static final class RecordingSessions implements SessionManager {
    String rotated;
    String revoked;

    public RotatedSession rotate(String refreshToken) {
      rotated = refreshToken;
      User user =
          User.register(
              UserId.random(),
              new Email("alice@example.com"),
              new PasswordHash(
                  "$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$hashhashhashhashhashhashhash"),
              "Alice",
              Instant.parse("2026-08-26T00:00:00Z"));
      return new RotatedSession(
          new SessionIssuer.IssuedSession("new-access", 900, "new-refresh"), user);
    }

    public void revoke(String refreshToken) {
      revoked = refreshToken;
    }
  }
}
