package com.creatorhub.identity.application.service;

import com.creatorhub.identity.application.port.in.LoginUseCase;
import com.creatorhub.identity.application.port.out.PasswordHasher;
import com.creatorhub.identity.application.port.out.SessionIssuer;
import com.creatorhub.identity.application.port.out.SessionIssuer.IssuedSession;
import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录应用服务，编排用户查询、密码验证、账号状态检查与会话签发。
 *
 * <p>用户不存在、账号禁用和密码错误统一返回 {@code INVALID_CREDENTIALS}，防止账户枚举。
 */
@Service
@Transactional
public class LoginService implements LoginUseCase {

  private final UserRepository users;
  private final PasswordHasher passwordHasher;
  private final SessionIssuer sessionIssuer;

  public LoginService(
      UserRepository users, PasswordHasher passwordHasher, SessionIssuer sessionIssuer) {
    this.users = users;
    this.passwordHasher = passwordHasher;
    this.sessionIssuer = sessionIssuer;
  }

  @Override
  public LoginResult login(LoginCommand command) {
    // 认证失败统一折叠为同一公开错误，不泄露失败发生在哪个步骤。
    User user =
        users
            .findByEmail(new Email(command.email()))
            .filter(User::canIssueSession)
            .filter(found -> passwordHasher.matches(command.password(), found.passwordHash()))
            .orElseThrow(LoginService::invalidCredentials);
    IssuedSession session = sessionIssuer.issue(user);
    return new LoginResult(
        session.accessToken(),
        session.expiresIn(),
        session.refreshToken(),
        user.id().value(),
        user.displayName(),
        null,
        user.role().name());
  }

  private static BusinessError invalidCredentials() {
    return new BusinessError("INVALID_CREDENTIALS", "Invalid email or password");
  }
}
