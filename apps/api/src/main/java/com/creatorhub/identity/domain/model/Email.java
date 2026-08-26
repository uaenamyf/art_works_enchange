package com.creatorhub.identity.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 用户邮箱值对象，也是唯一登录标识。
 *
 * <p>构造时去除首尾空白并使用 {@link Locale#ROOT} 转为小写，确保应用层检查、数据库唯一约束 和登录查询使用同一种规范化语义。
 */
public record Email(String value) {

  private static final int MAX_LENGTH = 254;
  private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  public Email {
    // 先规范化再校验，避免同一邮箱因大小写或空白绕过唯一性检查。
    value = normalize(value);
    if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid email address");
    }
  }

  private static String normalize(String value) {
    return Objects.requireNonNull(value, "email").trim().toLowerCase(Locale.ROOT);
  }
}
