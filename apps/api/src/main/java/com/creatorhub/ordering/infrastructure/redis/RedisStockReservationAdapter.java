package com.creatorhub.ordering.infrastructure.redis;

import com.creatorhub.catalog.application.port.in.InventoryStockReadPort;
import com.creatorhub.ordering.application.port.out.StockReservationGate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** 使用 Redis Lua 原子预检库存；缓存缺失或故障时降级到 PostgreSQL 条件更新。 */
@Component
public class RedisStockReservationAdapter implements StockReservationGate {

  private static final DefaultRedisScript<Long> RESERVE_SCRIPT =
      new DefaultRedisScript<>(
          """
          local current = redis.call('GET', KEYS[1])
          if not current then return 1 end
          if tonumber(current) < tonumber(ARGV[1]) then return 0 end
          redis.call('DECRBY', KEYS[1], ARGV[1])
          return 2
          """,
          Long.class);

  private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
      new DefaultRedisScript<>("redis.call('INCRBY', KEYS[1], ARGV[1]); return 1", Long.class);

  private final StringRedisTemplate redis;
  private final InventoryStockReadPort stockReader;

  public RedisStockReservationAdapter(StringRedisTemplate redis) {
    this(redis, null);
  }

  @Autowired
  public RedisStockReservationAdapter(
      StringRedisTemplate redis, InventoryStockReadPort stockReader) {
    this.redis = redis;
    this.stockReader = stockReader;
  }

  @Override
  public Reservation reserve(UUID artworkId, int quantity) {
    if (quantity < 1 || quantity > 99) {
      throw new IllegalArgumentException("quantity is invalid");
    }
    try {
      Long result =
          redis.execute(RESERVE_SCRIPT, List.of(key(artworkId)), String.valueOf(quantity));
      if (Long.valueOf(0).equals(result)) {
        return new Reservation(artworkId, quantity, false, true);
      }
      return new Reservation(artworkId, quantity, true, Long.valueOf(2).equals(result));
    } catch (RuntimeException ignored) {
      return new Reservation(artworkId, quantity, true, false);
    }
  }

  @Override
  public void release(Reservation reservation) {
    if (!reservation.cached()) return;
    try {
      redis.execute(
          RELEASE_SCRIPT,
          List.of(key(reservation.artworkId())),
          String.valueOf(reservation.quantity()));
    } catch (RuntimeException ignored) {
      // Redis 是可重建缓存，数据库事实源不会因补偿失败改变。
    }
  }

  /** 从 PostgreSQL 事实库存重建单个缓存键；不存在或非公开作品会清理陈旧缓存。 */
  public boolean rebuild(UUID artworkId) {
    if (stockReader == null) return false;
    java.util.OptionalInt stock = stockReader.findPublishedStock(artworkId);
    if (stock.isEmpty()) {
      redis.delete(key(artworkId));
      return false;
    }
    redis.opsForValue().set(key(artworkId), String.valueOf(stock.getAsInt()));
    return true;
  }

  private static String key(UUID artworkId) {
    return "creatorhub:inventory:available:" + artworkId;
  }
}
