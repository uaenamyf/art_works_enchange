package com.creatorhub.identity.application.port.out;

import com.creatorhub.identity.domain.model.PasswordHash;

/** 隔离密码算法与安全框架的输出端口，领域层不依赖 Spring Security。 */
public interface PasswordHasher {

  /** 对短生命周期明文进行 Argon2id 编码；实现不得记录或缓存明文。 */
  PasswordHash hash(String plaintextPassword);

  /** 使用算法提供的安全比较验证密码，调用方不得直接比较哈希字符串。 */
  boolean matches(String plaintextPassword, PasswordHash passwordHash);
}
