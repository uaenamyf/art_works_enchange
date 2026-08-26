package com.creatorhub.identity.application.service;

import com.creatorhub.identity.application.port.in.RevokeSessionUseCase;
import com.creatorhub.identity.application.port.out.SessionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 注销应用服务，在事务中撤销当前 Refresh 会话。 */
@Service
@Transactional
public class RevokeSessionService implements RevokeSessionUseCase {

  private final SessionManager sessions;

  public RevokeSessionService(SessionManager sessions) {
    this.sessions = sessions;
  }

  @Override
  public void revoke(String refreshToken) {
    sessions.revoke(refreshToken);
  }
}
