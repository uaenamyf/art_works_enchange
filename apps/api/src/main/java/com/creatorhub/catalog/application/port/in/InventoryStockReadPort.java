package com.creatorhub.catalog.application.port.in;

import java.util.OptionalInt;
import java.util.UUID;

/** 库存缓存重建读取端口；只暴露 PostgreSQL 事实库存，不允许上层访问 Catalog Mapper。 */
public interface InventoryStockReadPort {

  OptionalInt findPublishedStock(UUID artworkId);
}
