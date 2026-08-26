package com.creatorhub.identity.application.service;

import com.creatorhub.identity.application.port.in.RotateSessionUseCase;
import com.creatorhub.identity.application.port.out.SessionManager;
import com.creatorhub.identity.application.port.out.SessionManager.RotatedSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会话轮换应用服务，事务边界覆盖旧会话撤销与新会话签发。 */
@Service
@Transactional
public class RotateSessionService implements RotateSessionUseCase {

  private final SessionManager sessions;

  public RotateSessionService(SessionManager sessions) {
    this.sessions = sessions;
  }

  @Override
  public RotateResult rotate(String refreshToken) {
    RotatedSession rotated = sessions.rotate(refreshToken);
    var issued = rotated.session();
    var user = rotated.user();
    return new RotateResult(
        issued.accessToken(),
        issued.expiresIn(),
        issued.refreshToken(),
        user.id().value(),
        user.displayName(),
        null,
        user.role().name());
  }
}
