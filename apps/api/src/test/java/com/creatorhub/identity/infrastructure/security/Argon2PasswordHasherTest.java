package com.creatorhub.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 验证真实 Argon2id 编码、正确密码匹配和明文不进入哈希结果。 */
class Argon2PasswordHasherTest {

  @Test
  void hashesWithArgon2idAndVerifiesWithoutStoringPlaintext() {
    Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    var hash = hasher.hash("very-secure-password");

    assertThat(hash.value()).startsWith("$argon2id$").doesNotContain("very-secure-password");
    assertThat(hasher.matches("very-secure-password", hash)).isTrue();
    assertThat(hasher.matches("wrong-password", hash)).isFalse();
  }
}
