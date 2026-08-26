package com.creatorhub.creator.domain.model;

import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Creator 上下文聚合根，维护公开档案、稳定 slug 以及绑定 UserId 的不变量。
 *
 * <p>一个 UserId 只能绑定一个 Creator，最终由领域仓储检查和数据库唯一约束共同保证。
 */
public final class Creator extends AggregateRoot<CreatorId> {

  private static final Pattern SLUG = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,38}[a-z0-9])?$");

  private final UserId userId;
  private final String slug;
  private String displayName;
  private String category;
  private String bio;
  private String location;
  private final Instant createdAt;

  private Creator(
      CreatorId id,
      UserId userId,
      String slug,
      String displayName,
      String category,
      String bio,
      String location,
      Instant createdAt) {
    super(id);
    this.userId = Objects.requireNonNull(userId, "userId");
    this.slug = requireSlug(slug);
    updateProfile(displayName, category, bio, location);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  /** 创建创作者档案；调用方不得指定其他用户作为 owner。 */
  public static Creator apply(
      CreatorId id,
      UserId userId,
      String slug,
      String displayName,
      String category,
      String bio,
      String location,
      Instant createdAt) {
    return new Creator(id, userId, slug, displayName, category, bio, location, createdAt);
  }

  /** 仅供仓储适配器恢复聚合持久化状态。 */
  public static Creator reconstitute(
      CreatorId id,
      UserId userId,
      String slug,
      String displayName,
      String category,
      String bio,
      String location,
      Instant createdAt) {
    return new Creator(id, userId, slug, displayName, category, bio, location, createdAt);
  }

  /** 更新可变公开档案；slug 和 owner 创建后保持不变。 */
  public void updateProfile(String displayName, String category, String bio, String location) {
    this.displayName = requireLength(displayName, "displayName", 2, 50);
    this.category = requireLength(category, "category", 1, 50);
    this.bio = requireLength(bio, "bio", 0, 2000);
    this.location = normalizeOptional(location, 100);
  }

  public UserId userId() {
    return userId;
  }

  public String slug() {
    return slug;
  }

  public String displayName() {
    return displayName;
  }

  public String category() {
    return category;
  }

  public String bio() {
    return bio;
  }

  public String location() {
    return location;
  }

  public Instant createdAt() {
    return createdAt;
  }

  private static String requireSlug(String slug) {
    String normalized = Objects.requireNonNull(slug, "slug").trim();
    if (!SLUG.matcher(normalized).matches()) {
      throw new IllegalArgumentException("Invalid creator slug");
    }
    return normalized;
  }

  private static String requireLength(String value, String name, int min, int max) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.length() < min || normalized.length() > max) {
      throw new IllegalArgumentException(name + " length is invalid");
    }
    return normalized;
  }

  private static String normalizeOptional(String value, int max) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > max) {
      throw new IllegalArgumentException("location length is invalid");
    }
    return normalized;
  }
}
