package com.creatorhub.identity.application.service;

import com.creatorhub.identity.application.port.in.RegisterUserUseCase;
import com.creatorhub.identity.application.port.out.PasswordHasher;
import com.creatorhub.identity.application.port.out.SessionIssuer;
import com.creatorhub.identity.application.port.out.SessionIssuer.IssuedSession;
import com.creatorhub.identity.domain.model.Email;
import com.creatorhub.identity.domain.model.User;
import com.creatorhub.identity.domain.repository.UserRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.error.BusinessError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册应用服务，在单一事务中编排邮箱唯一性、密码哈希、聚合保存和首个会话签发。
 *
 * <p>明文密码只短暂存在于命令对象，并立即交给 {@link PasswordHasher}；不会进入聚合或仓储。
 */
@Service
@Transactional
public class RegisterUserService implements RegisterUserUseCase {

  private final UserRepository users;
  private final PasswordHasher passwordHasher;
  private final SessionIssuer sessionIssuer;

  public RegisterUserService(
      UserRepository users, PasswordHasher passwordHasher, SessionIssuer sessionIssuer) {
    this.users = users;
    this.passwordHasher = passwordHasher;
    this.sessionIssuer = sessionIssuer;
  }

  @Override
  public RegistrationResult register(RegisterCommand command) {
    Email email = new Email(command.email());
    validatePlaintextPassword(command.password());
    if (users.existsByEmail(email)) {
      throw new BusinessError("EMAIL_TAKEN", "Email is already registered");
    }

    // 应用层检查提供友好错误；数据库唯一约束仍是并发注册时的最终保障。
    User user =
        User.register(
            UserId.random(),
            email,
            passwordHasher.hash(command.password()),
            command.displayName(),
            AppClock.now());
    User saved = users.save(user);
    IssuedSession session = sessionIssuer.issue(saved);

    return new RegistrationResult(
        session.accessToken(),
        session.expiresIn(),
        session.refreshToken(),
        saved.id().value(),
        saved.displayName(),
        null,
        saved.role().name());
  }

  private static void validatePlaintextPassword(String password) {
    if (password == null || password.length() < 10 || password.length() > 128) {
      throw new BusinessError(
          "VALIDATION_FAILED", "Password must be between 10 and 128 characters");
    }
  }
}
