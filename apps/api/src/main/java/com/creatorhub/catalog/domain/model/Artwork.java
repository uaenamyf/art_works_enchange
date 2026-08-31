package com.creatorhub.catalog.domain.model;

import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AggregateRoot;
import com.creatorhub.sharedkernel.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Artwork 聚合根，负责草稿编辑、审核流转和发布门禁。
 *
 * <p>发布不是普通字段赋值：必须先有媒体、有效价格、正库存，并且审核状态为 APPROVED。 审核拒绝或封禁的作品不能通过旧版本请求直接重新上线。
 */
public final class Artwork extends AggregateRoot<ArtworkId> {

  private final UserId ownerId;
  private final UUID creatorId;
  private String title;
  private String description;
  private String category;
  private Money price;
  private int stock;
  private ProductType productType;
  private List<UUID> mediaIds;
  private ArtworkStatus status;
  private long version;
  private final Instant createdAt;
  private Instant updatedAt;

  private Artwork(
      ArtworkId id,
      UserId ownerId,
      UUID creatorId,
      String title,
      String description,
      String category,
      Money price,
      int stock,
      List<UUID> mediaIds,
      Instant now,
      ProductType productType) {
    super(id);
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    this.creatorId = Objects.requireNonNull(creatorId, "creatorId");
    this.createdAt = Objects.requireNonNull(now, "now");
    this.status = ArtworkStatus.DRAFT;
    this.productType = Objects.requireNonNull(productType, "productType");
    update(title, description, category, price, stock, mediaIds, now);
  }

  /** 创建草稿；创建者身份由认证上下文提供，客户端不能指定 owner。 */
  public static Artwork draft(
      ArtworkId id,
      UserId ownerId,
      UUID creatorId,
      String title,
      String description,
      String category,
      Money price,
      int stock,
      List<UUID> mediaIds,
      Instant now) {
    return draft(
        id,
        ownerId,
        creatorId,
        title,
        description,
        category,
        price,
        stock,
        mediaIds,
        now,
        ProductType.PHYSICAL);
  }

  public static Artwork draft(
      ArtworkId id,
      UserId ownerId,
      UUID creatorId,
      String title,
      String description,
      String category,
      Money price,
      int stock,
      List<UUID> mediaIds,
      Instant now,
      ProductType productType) {
    return new Artwork(
        id,
        ownerId,
        creatorId,
        title,
        description,
        category,
        price,
        stock,
        mediaIds,
        now,
        productType);
  }

  /** 仅供仓储适配器恢复完整作品状态；恢复不会触发新的业务版本递增。 */
  public static Artwork reconstitute(
      ArtworkId id,
      UserId ownerId,
      UUID creatorId,
      String title,
      String description,
      String category,
      Money price,
      int stock,
      List<UUID> mediaIds,
      ArtworkStatus status,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    return reconstitute(
        id,
        ownerId,
        creatorId,
        title,
        description,
        category,
        price,
        stock,
        mediaIds,
        status,
        version,
        createdAt,
        updatedAt,
        ProductType.PHYSICAL);
  }

  public static Artwork reconstitute(
      ArtworkId id,
      UserId ownerId,
      UUID creatorId,
      String title,
      String description,
      String category,
      Money price,
      int stock,
      List<UUID> mediaIds,
      ArtworkStatus status,
      long version,
      Instant createdAt,
      Instant updatedAt,
      ProductType productType) {
    Artwork artwork =
        new Artwork(
            id,
            ownerId,
            creatorId,
            title,
            description,
            category,
            price,
            stock,
            mediaIds,
            createdAt,
            productType);
    artwork.status = Objects.requireNonNull(status, "status");
    artwork.version = version;
    artwork.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    return artwork;
  }

  /** 修改草稿或被拒绝作品；已发布、封禁和归档作品不能绕过状态机编辑。 */
  public void update(
      String title,
      String description,
      String category,
      Money price,
      int stock,
      List<UUID> mediaIds,
      Instant now) {
    if (status != ArtworkStatus.DRAFT && status != ArtworkStatus.REJECTED) {
      throw new IllegalStateException("Artwork is not editable");
    }
    this.title = requireLength(title, 2, 160, "title");
    this.description = requireLength(description, 10, 10000, "description");
    this.category = requireLength(category, 1, 50, "category");
    this.price = Objects.requireNonNull(price, "price");
    if (!price.isPositive()) {
      throw new IllegalArgumentException("Price must be positive");
    }
    if (stock < 0 || stock > 100000) {
      throw new IllegalArgumentException("Stock is invalid");
    }
    this.stock = stock;
    replaceMedia(mediaIds);
    this.updatedAt = Objects.requireNonNull(now, "now");
    if (status == ArtworkStatus.REJECTED) {
      status = ArtworkStatus.DRAFT;
    }
    version++;
  }

  /** 替换作品媒体集合；媒体 ID 必须唯一且数量不超过 12 张。 */
  public void replaceMedia(List<UUID> mediaIds) {
    List<UUID> normalized = List.copyOf(Objects.requireNonNull(mediaIds, "mediaIds"));
    if (normalized.size() > 12
        || normalized.stream().anyMatch(Objects::isNull)
        || normalized.stream().distinct().count() != normalized.size()) {
      throw new IllegalArgumentException("Media set is invalid");
    }
    this.mediaIds = normalized;
  }

  /** Creator 提交审核；没有媒体的作品不能进入审核队列。 */
  public void submitForReview(Instant now) {
    if (status != ArtworkStatus.DRAFT || mediaIds.isEmpty()) {
      throw new IllegalStateException("Artwork requires media before review");
    }
    status = ArtworkStatus.PENDING_REVIEW;
    touch(now);
  }

  /** 审核通过；只有待审作品可被审核通过。 */
  public void approve(Instant now) {
    requireStatus(ArtworkStatus.PENDING_REVIEW);
    status = ArtworkStatus.APPROVED;
    touch(now);
  }

  /** 审核拒绝；拒绝原因由审核记录另行持久化，聚合只维护不可公开状态。 */
  public void reject(Instant now) {
    requireStatus(ArtworkStatus.PENDING_REVIEW);
    status = ArtworkStatus.REJECTED;
    touch(now);
  }

  /** 仅审核通过的作品可公开发布，防止 Controller 或客户端跳过审核。 */
  public void publish(Instant now) {
    requireStatus(ArtworkStatus.APPROVED);
    if (mediaIds.isEmpty()
        || !price.isPositive()
        || (productType == ProductType.PHYSICAL && stock <= 0)) {
      throw new IllegalStateException("Artwork is not ready to publish");
    }
    status = ArtworkStatus.PUBLISHED;
    touch(now);
  }

  /** 审核关闭时，Creator 仍必须满足媒体、价格和实体库存门禁后才能直接上架。 */
  public void publishWithoutReview(Instant now) {
    requireStatus(ArtworkStatus.DRAFT, ArtworkStatus.REJECTED);
    if (mediaIds.isEmpty()
        || !price.isPositive()
        || (productType == ProductType.PHYSICAL && stock <= 0)) {
      throw new IllegalStateException("Artwork is not ready to publish");
    }
    status = ArtworkStatus.PUBLISHED;
    touch(now);
  }

  /** 已发布作品可以下架归档，归档后不可重新发布。 */
  public void archive(Instant now) {
    requireStatus(ArtworkStatus.PUBLISHED, ArtworkStatus.SOLD_OUT);
    status = ArtworkStatus.ARCHIVED;
    touch(now);
  }

  /** 审核复查确认违规后封禁作品；封禁只能从公开或已审核状态进入，禁止重新发布。 */
  public void block(Instant now) {
    requireStatus(ArtworkStatus.APPROVED, ArtworkStatus.PUBLISHED, ArtworkStatus.SOLD_OUT);
    status = ArtworkStatus.BLOCKED;
    touch(now);
  }

  private void requireStatus(ArtworkStatus... allowed) {
    for (ArtworkStatus candidate : allowed) {
      if (status == candidate) {
        return;
      }
    }
    throw new IllegalStateException("Artwork status does not allow this operation");
  }

  private void touch(Instant now) {
    updatedAt = Objects.requireNonNull(now, "now");
    version++;
  }

  private static String requireLength(String value, int min, int max, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.length() < min || normalized.length() > max) {
      throw new IllegalArgumentException(name + " length is invalid");
    }
    return normalized;
  }

  public UserId ownerId() {
    return ownerId;
  }

  public UUID creatorId() {
    return creatorId;
  }

  public String title() {
    return title;
  }

  public String description() {
    return description;
  }

  public String category() {
    return category;
  }

  public Money price() {
    return price;
  }

  public int stock() {
    return stock;
  }

  public ProductType productType() {
    return productType;
  }

  public List<UUID> mediaIds() {
    return mediaIds;
  }

  public ArtworkStatus status() {
    return status;
  }

  public long version() {
    return version;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
