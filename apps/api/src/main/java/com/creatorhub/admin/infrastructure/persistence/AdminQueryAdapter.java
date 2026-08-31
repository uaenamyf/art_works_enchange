package com.creatorhub.admin.infrastructure.persistence;

import com.creatorhub.admin.application.port.in.AdminUseCases;
import com.creatorhub.admin.application.port.out.AdminQuery;
import com.creatorhub.catalog.domain.model.ArtworkStatus;
import com.creatorhub.catalog.domain.model.ProductType;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Admin 查询适配器；将数据库行转换为只读视图，并使用稳定游标避免分页漂移。 */
@Repository
public class AdminQueryAdapter implements AdminQuery {

  private final AdminQueryMapper mapper;

  public AdminQueryAdapter(AdminQueryMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public AdminUseCases.ReviewQueuePage findReviewQueue(String cursor, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    Cursor decoded = Cursor.decode(cursor);
    List<AdminQueryMapper.ReviewQueueRow> rows =
        mapper.findReviewQueue(decoded.updatedAt(), decoded.id(), pageSize + 1);
    boolean hasMore = rows.size() > pageSize;
    List<AdminQueryMapper.ReviewQueueRow> page = rows.stream().limit(pageSize).toList();
    String nextCursor = hasMore ? Cursor.encode(page.get(page.size() - 1)) : null;
    return new AdminUseCases.ReviewQueuePage(
        page.stream().map(this::toItem).toList(), nextCursor, hasMore);
  }

  @Override
  public AdminUseCases.ReviewQueueItem findReviewArtwork(UUID artworkId) {
    AdminQueryMapper.ReviewQueueRow row = mapper.findReviewArtwork(artworkId);
    if (row == null) {
      throw new BusinessError("ARTWORK_NOT_FOUND", "Artwork is not pending review");
    }
    return toItem(row);
  }

  @Override
  public AdminUseCases.ReviewHistoryPage findReviewHistory(String cursor, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 50);
    HistoryCursor decoded = HistoryCursor.decode(cursor);
    List<AdminQueryMapper.ReviewHistoryRow> rows =
        mapper.findReviewHistory(decoded.createdAt(), decoded.id(), pageSize + 1);
    boolean hasMore = rows.size() > pageSize;
    List<AdminQueryMapper.ReviewHistoryRow> page = rows.stream().limit(pageSize).toList();
    String nextCursor = hasMore ? HistoryCursor.encode(page.get(page.size() - 1)) : null;
    return new AdminUseCases.ReviewHistoryPage(
        page.stream().map(this::toHistoryItem).toList(), nextCursor, hasMore);
  }

  @Override
  public AdminUseCases.DashboardView findDashboard() {
    AdminQueryMapper.DashboardTotalsRow totals = mapper.findDashboardTotals();
    Map<String, Long> artworks = toMap(mapper.countArtworksByStatus());
    Map<String, Long> orders = toMap(mapper.countOrdersByStatus());
    return new AdminUseCases.DashboardView(
        totals.userCount(),
        totals.creatorCount(),
        totals.publishedArtworkCount(),
        totals.pendingReviewArtworkCount(),
        artworks,
        orders);
  }

  private AdminUseCases.ReviewQueueItem toItem(AdminQueryMapper.ReviewQueueRow row) {
    return new AdminUseCases.ReviewQueueItem(
        row.id(),
        row.title(),
        row.description(),
        row.category(),
        ProductType.valueOf(row.productType()),
        row.priceAmount(),
        row.priceCurrency(),
        row.stock(),
        new AdminUseCases.CreatorSnapshot(
            row.creatorId(), row.creatorSlug(), row.creatorDisplayName(), row.creatorCategory()),
        mapper.findMediaIds(row.id()),
        ArtworkStatus.valueOf(row.status()),
        row.updatedAt());
  }

  private AdminUseCases.ReviewHistoryItem toHistoryItem(AdminQueryMapper.ReviewHistoryRow row) {
    return new AdminUseCases.ReviewHistoryItem(
        row.id(),
        row.artworkId(),
        row.artworkTitle(),
        new AdminUseCases.CreatorSnapshot(
            row.creatorId(), row.creatorSlug(), row.creatorDisplayName(), row.creatorCategory()),
        row.reviewerId(),
        ArtworkStatus.valueOf(row.decision()),
        row.reason(),
        row.createdAt());
  }

  private static Map<String, Long> toMap(List<AdminQueryMapper.StatusCountRow> rows) {
    Map<String, Long> result = new LinkedHashMap<>();
    for (AdminQueryMapper.StatusCountRow row : rows) {
      result.put(row.status(), row.count());
    }
    return result;
  }

  private record Cursor(Instant updatedAt, UUID id) {
    private static Cursor decode(String value) {
      if (value == null || value.isBlank()) return new Cursor(null, null);
      try {
        String decoded = new String(Base64.getUrlDecoder().decode(value));
        String[] parts = decoded.split("\\|", 2);
        return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
      } catch (RuntimeException exception) {
        throw new BusinessError("INVALID_CURSOR", "Cursor is invalid");
      }
    }

    private static String encode(AdminQueryMapper.ReviewQueueRow row) {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString((row.updatedAt().toString() + "|" + row.id()).getBytes());
    }
  }

  private record HistoryCursor(Instant createdAt, UUID id) {
    private static HistoryCursor decode(String value) {
      if (value == null || value.isBlank()) return new HistoryCursor(null, null);
      try {
        String decoded = new String(Base64.getUrlDecoder().decode(value));
        String[] parts = decoded.split("\\|", 2);
        return new HistoryCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
      } catch (RuntimeException exception) {
        throw new BusinessError("INVALID_CURSOR", "Cursor is invalid");
      }
    }

    private static String encode(AdminQueryMapper.ReviewHistoryRow row) {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString((row.createdAt().toString() + "|" + row.id()).getBytes());
    }
  }
}
