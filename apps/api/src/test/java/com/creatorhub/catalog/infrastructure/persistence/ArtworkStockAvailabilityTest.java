package com.creatorhub.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证库存适配器拒绝非法数量，并把数据库条件更新结果转换成业务布尔值。 */
class ArtworkStockAvailabilityTest {

  @Test
  void returnsFalseWhenDatabaseConditionDoesNotReserveStock() {
    ArtworkMapper mapper = mock(ArtworkMapper.class);
    InventoryMovementMapper inventory = mock(InventoryMovementMapper.class);
    ArtworkRepositoryAdapter adapter =
        new ArtworkRepositoryAdapter(mapper, mock(ArtworkMediaMapper.class), inventory);
    UUID artworkId = UUID.randomUUID();
    when(mapper.reserveStock(artworkId, 2)).thenReturn(0);

    assertThat(adapter.reserve(artworkId, 2, UUID.randomUUID())).isFalse();
  }

  @Test
  void returnsTrueWhenDatabaseConditionReservesStock() {
    ArtworkMapper mapper = mock(ArtworkMapper.class);
    InventoryMovementMapper inventory = mock(InventoryMovementMapper.class);
    ArtworkRepositoryAdapter adapter =
        new ArtworkRepositoryAdapter(mapper, mock(ArtworkMediaMapper.class), inventory);
    UUID artworkId = UUID.randomUUID();
    when(mapper.reserveStock(artworkId, 2)).thenReturn(1);

    when(inventory.record(any(), any(), any(), any(), any(Integer.class), any())).thenReturn(1);
    assertThat(adapter.reserve(artworkId, 2, UUID.randomUUID())).isTrue();
  }

  @Test
  void rejectsInvalidQuantityBeforeDatabaseCall() {
    ArtworkRepositoryAdapter adapter =
        new ArtworkRepositoryAdapter(
            mock(ArtworkMapper.class),
            mock(ArtworkMediaMapper.class),
            mock(InventoryMovementMapper.class));

    assertThatThrownBy(() -> adapter.reserve(UUID.randomUUID(), 0, UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
