package com.creatorhub.admin.application.port.in;

import com.creatorhub.catalog.domain.model.ArtworkStatus;
import com.creatorhub.catalog.domain.model.ProductType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Admin 只读运营查询端口；审核写操作仍复用 Catalog 的审核用例。 */
public interface AdminUseCases {

  ReviewQueuePage listReviewQueue(String cursor, int limit);

  ReviewQueueItem getReviewArtwork(UUID artworkId);

  ReviewHistoryPage listReviewHistory(String cursor, int limit);

  DashboardView dashboard();

  record ReviewQueuePage(List<ReviewQueueItem> items, String nextCursor, boolean hasMore) {}

  record ReviewHistoryPage(List<ReviewHistoryItem> items, String nextCursor, boolean hasMore) {}

  record ReviewQueueItem(
      UUID id,
      String title,
      String description,
      String category,
      ProductType productType,
      BigDecimal priceAmount,
      String priceCurrency,
      int stock,
      CreatorSnapshot creator,
      List<UUID> mediaIds,
      ArtworkStatus status,
      Instant updatedAt) {}

  record CreatorSnapshot(UUID id, String slug, String displayName, String category) {}

  record ReviewHistoryItem(
      UUID id,
      UUID artworkId,
      String artworkTitle,
      CreatorSnapshot creator,
      UUID reviewerId,
      ArtworkStatus decision,
      String reason,
      Instant createdAt) {}

  record DashboardView(
      long userCount,
      long creatorCount,
      long publishedArtworkCount,
      long pendingReviewArtworkCount,
      Map<String, Long> artworkCountByStatus,
      Map<String, Long> orderCountByStatus) {}
}
