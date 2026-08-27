package com.creatorhub.follow.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;

/** 关注关系持久化行，仅供 Follow infrastructure 层使用。 */
@TableName("creator_follows")
class FollowPersistenceRow {

  private UUID userId;
  private UUID creatorId;
  private Instant createdAt;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(UUID creatorId) {
    this.creatorId = creatorId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
