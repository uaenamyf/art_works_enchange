package com.creatorhub.identity.domain.model;

import java.util.Objects;

/**
 * Argon2id 密码哈希值对象。
 *
 * <p>领域模型只接受已编码值，拒绝明文和其他格式，保证明文密码不会进入聚合、仓储或日志。
 */
public record PasswordHash(String value) {

  public PasswordHash {
    value = Objects.requireNonNull(value, "passwordHash").trim();
    // 格式守卫是领域边界的最后防线；算法参数由基础设施适配器集中管理。
    if (!value.startsWith("$argon2id$") || value.length() < 32) {
      throw new IllegalArgumentException("Password hash must be Argon2id encoded");
    }
  }
}
