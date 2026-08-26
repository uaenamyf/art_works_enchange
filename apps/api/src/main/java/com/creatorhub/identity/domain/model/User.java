package com.creatorhub.identity.domain.model;

import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AggregateRoot;
import java.time.Instant;
import java.util.Objects;

/**
 * Identity 上下文的用户聚合根，维护登录资格、角色与基础公开资料不变量。
 *
 * <p>新注册用户固定为启用状态的 {@link UserRole#USER}；禁用用户不能签发会话或升级为创作者。
 */
public final class User extends AggregateRoot<UserId> {

  private final Email email;
  private PasswordHash passwordHash;
  private String displayName;
  private UserRole role;
  private boolean disabled;
  private final Instant createdAt;

  private User(
      UserId id,
      Email email,
      PasswordHash passwordHash,
      String displayName,
      UserRole role,
      boolean disabled,
      Instant createdAt) {
    super(id);
    this.email = Objects.requireNonNull(email, "email");
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    this.displayName = requireDisplayName(displayName);
    this.role = Objects.requireNonNull(role, "role");
    this.disabled = disabled;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  public static User register(
      UserId id, Email email, PasswordHash passwordHash, String displayName, Instant createdAt) {
    // 注册入口不允许调用方指定角色或禁用状态，防止越权创建高权限账号。
    return new User(id, email, passwordHash, displayName, UserRole.USER, false, createdAt);
  }

  /** 仅供仓储适配器从持久化状态恢复聚合，不用于创建新用户。 */
  public static User reconstitute(
      UserId id,
      Email email,
      PasswordHash passwordHash,
      String displayName,
      UserRole role,
      boolean disabled,
      Instant createdAt) {
    return new User(id, email, passwordHash, displayName, role, disabled, createdAt);
  }

  /** 将已通过 Creator 入驻用例校验的用户提升为创作者；禁用用户不能提升。 */
  public void promoteToCreator() {
    if (disabled) {
      throw new IllegalStateException("Disabled user cannot become a creator");
    }
    if (role == UserRole.ADMIN) {
      return;
    }
    role = UserRole.CREATOR;
  }

  /** 禁用账号；禁用后所有认证用例必须拒绝签发新会话。 */
  public void disable() {
    disabled = true;
  }

  /** 返回当前账号是否具备签发会话的领域资格。 */
  public boolean canIssueSession() {
    return !disabled;
  }

  public Email email() {
    return email;
  }

  public PasswordHash passwordHash() {
    return passwordHash;
  }

  public String displayName() {
    return displayName;
  }

  public UserRole role() {
    return role;
  }

  public boolean disabled() {
    return disabled;
  }

  public Instant createdAt() {
    return createdAt;
  }

  private static String requireDisplayName(String displayName) {
    String normalized = Objects.requireNonNull(displayName, "displayName").trim();
    if (normalized.length() < 2 || normalized.length() > 50) {
      throw new IllegalArgumentException("Display name must be between 2 and 50 characters");
    }
    return normalized;
  }
}
