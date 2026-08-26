package com.creatorhub.identity.infrastructure.security;

import com.creatorhub.identity.application.port.out.PasswordHasher;
import com.creatorhub.identity.domain.model.PasswordHash;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Spring Security Argon2id 密码适配器，集中管理算法参数并实现应用层密码端口。
 *
 * <p>实现不记录、不缓存明文密码，编码结果必须满足领域 {@code PasswordHash} 的格式约束。
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

  private final Argon2PasswordEncoder encoder =
      Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

  @Override
  public PasswordHash hash(String plaintextPassword) {
    return new PasswordHash(encoder.encode(plaintextPassword));
  }

  @Override
  public boolean matches(String plaintextPassword, PasswordHash passwordHash) {
    return encoder.matches(plaintextPassword, passwordHash.value());
  }
}
