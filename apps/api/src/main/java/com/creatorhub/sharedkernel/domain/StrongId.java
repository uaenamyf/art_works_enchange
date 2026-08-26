package com.creatorhub.sharedkernel.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 强类型 ID 抽象。
 *
 * <p>跨上下文协作只允许传递强类型 ID 或不可变值快照，禁止跨上下文访问 Mapper、Row 或数据表。 子类必须实现 {@link #value()} 与 {@link
 * #equals(Object)}。
 */
public abstract class StrongId {

  private final UUID value;

  protected StrongId(UUID value) {
    this.value = Objects.requireNonNull(value, "id value");
  }

  public static UUID randomUuid() {
    return UUID.randomUUID();
  }

  public UUID value() {
    return value;
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public final boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof StrongId that)) {
      return false;
    }
    return Objects.equals(value, that.value);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + value + "]";
  }
}
