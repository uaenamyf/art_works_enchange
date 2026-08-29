package com.creatorhub.catalog.infrastructure.persistence;

import com.creatorhub.catalog.application.port.in.InventoryStockReadPort;
import com.creatorhub.catalog.application.port.in.StockAvailabilityPort;
import com.creatorhub.catalog.domain.model.Artwork;
import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.catalog.domain.repository.ArtworkRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import com.creatorhub.sharedkernel.domain.Money;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Artwork 聚合仓储适配器，显式转换领域模型和 MyBatis 持久化行。 */
@Repository
public class ArtworkRepositoryAdapter
    implements ArtworkRepository, StockAvailabilityPort, InventoryStockReadPort {

  private final ArtworkMapper mapper;
  private final ArtworkMediaMapper mediaMapper;
  private final InventoryMovementMapper inventoryMovements;

  public ArtworkRepositoryAdapter(
      ArtworkMapper mapper,
      ArtworkMediaMapper mediaMapper,
      InventoryMovementMapper inventoryMovements) {
    this.mapper = mapper;
    this.mediaMapper = mediaMapper;
    this.inventoryMovements = inventoryMovements;
  }

  @Override
  public Optional<Artwork> findById(ArtworkId id) {
    return Optional.ofNullable(mapper.findByIdValue(id.value()))
        .map(row -> toDomain(row, mediaMapper.findIdsByArtworkId(row.getId())));
  }

  @Override
  public Optional<Artwork> findPublishedById(ArtworkId id) {
    return Optional.ofNullable(mapper.findPublishedById(id.value()))
        .map(row -> toDomain(row, mediaMapper.findIdsByArtworkId(row.getId())));
  }

  @Override
  public Optional<Artwork> findByOwnerId(UserId ownerId, ArtworkId id) {
    return Optional.ofNullable(mapper.findByOwnerId(ownerId.value(), id.value()))
        .map(row -> toDomain(row, mediaMapper.findIdsByArtworkId(row.getId())));
  }

  @Override
  public List<Artwork> findPublishedPage(PublishedPageQuery query) {
    return mapper
        .findPublishedPage(
            query.query(),
            query.category(),
            query.creatorId(),
            query.beforeCreatedAt(),
            query.beforeId() == null ? null : query.beforeId().value(),
            query.limit())
        .stream()
        .map(row -> toDomain(row, mediaMapper.findIdsByArtworkId(row.getId())))
        .toList();
  }

  @Override
  public List<Artwork> findOwnedPage(OwnedPageQuery query) {
    return mapper
        .findOwnedPage(
            query.ownerId().value(),
            query.status(),
            query.beforeUpdatedAt(),
            query.beforeId() == null ? null : query.beforeId().value(),
            query.limit())
        .stream()
        .map(row -> toDomain(row, mediaMapper.findIdsByArtworkId(row.getId())))
        .toList();
  }

  @Override
  public Artwork save(Artwork artwork) {
    ArtworkPersistenceRow row = mapper.findByIdValue(artwork.id().value());
    boolean inserting = row == null;
    if (inserting) {
      row = new ArtworkPersistenceRow();
      row.setId(artwork.id().value());
    }
    row.setOwnerId(artwork.ownerId().value());
    row.setCreatorId(artwork.creatorId());
    row.setTitle(artwork.title());
    row.setDescription(artwork.description());
    row.setCategory(artwork.category());
    row.setPriceAmount(artwork.price().amount());
    row.setPriceCurrency(artwork.price().currency().getCurrencyCode());
    row.setStock(artwork.stock());
    row.setProductType(artwork.productType());
    row.setStatus(artwork.status());
    row.setVersion(artwork.version());
    row.setCreatedAt(artwork.createdAt());
    row.setUpdatedAt(AppClock.now());
    if (inserting) {
      mapper.insert(row);
    } else if (mapper.updateById(row) != 1) {
      throw new IllegalStateException("Artwork optimistic lock conflict");
    }
    mediaMapper.deleteByArtworkId(row.getId());
    for (int index = 0; index < artwork.mediaIds().size(); index++) {
      if (mediaMapper.bindReadyMedia(
              row.getId(), artwork.mediaIds().get(index), artwork.ownerId().value(), index)
          != 1) {
        throw new BusinessError("MEDIA_NOT_READY_OR_NOT_OWNED", "Artwork media is not ready");
      }
    }
    return toDomain(mapper.findByIdValue(row.getId()), mediaMapper.findIdsByArtworkId(row.getId()));
  }

  @Override
  public java.util.OptionalInt findPublishedStock(UUID artworkId) {
    Integer stock = mapper.findPublishedStock(artworkId);
    return stock == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(stock);
  }

  @Override
  @Transactional
  public boolean reserve(UUID artworkId, int quantity, UUID reservationId) {
    if (quantity < 1 || quantity > 99) {
      throw new IllegalArgumentException("quantity is invalid");
    }
    if (mapper.reserveStock(artworkId, quantity) != 1) {
      return false;
    }
    if (inventoryMovements.record(
            UUID.randomUUID(), reservationId, artworkId, "RESERVE", quantity, AppClock.now())
        != 1) {
      throw new IllegalStateException("Inventory reservation record failed");
    }
    return true;
  }

  @Override
  @Transactional
  public boolean release(UUID artworkId, int quantity, UUID reservationId) {
    if (quantity < 1 || quantity > 99) {
      throw new IllegalArgumentException("quantity is invalid");
    }
    if (inventoryMovements.record(
            UUID.randomUUID(), reservationId, artworkId, "RELEASE", quantity, AppClock.now())
        != 1) {
      return false;
    }
    if (mapper.releaseStock(artworkId, quantity) != 1) {
      throw new IllegalStateException("Artwork not found while releasing stock");
    }
    return true;
  }

  private static Artwork toDomain(
      ArtworkPersistenceRow row, java.util.List<java.util.UUID> mediaIds) {
    return Artwork.reconstitute(
        ArtworkId.of(row.getId()),
        UserId.of(row.getOwnerId()),
        row.getCreatorId(),
        row.getTitle(),
        row.getDescription(),
        row.getCategory(),
        Money.of(row.getPriceAmount().toPlainString(), row.getPriceCurrency()),
        row.getStock(),
        mediaIds,
        row.getStatus(),
        row.getVersion(),
        row.getCreatedAt(),
        row.getUpdatedAt(),
        row.getProductType() == null
            ? com.creatorhub.catalog.domain.model.ProductType.PHYSICAL
            : row.getProductType());
  }
}
