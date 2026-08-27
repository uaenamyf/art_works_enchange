package com.creatorhub.collection.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;

/** 收藏关系持久化行，仅供 Collection infrastructure 层使用。 */
@TableName("favorite_collections")
class FavoritePersistenceRow {

  private UUID userId;
  private UUID artworkId;
}
