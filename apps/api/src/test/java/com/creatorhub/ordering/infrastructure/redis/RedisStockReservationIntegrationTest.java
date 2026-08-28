package com.creatorhub.ordering.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.creatorhub.catalog.application.port.in.InventoryStockReadPort;
import com.creatorhub.ordering.application.port.out.StockReservationGate.Reservation;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** 使用真实 Redis 验证 Lua 库存闸门、释放补偿和 PostgreSQL 库存缓存重建。 */
@Testcontainers
class RedisStockReservationIntegrationTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:8-alpine")).withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redis;
  private RedisStockReservationAdapter reservations;

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    redis = new StringRedisTemplate(connectionFactory);
    redis.afterPropertiesSet();
    InventoryStockReadPort stockReader = mock(InventoryStockReadPort.class);
    reservations = new RedisStockReservationAdapter(redis, stockReader);
  }

  @AfterEach
  void tearDown() {
    redis.getConnectionFactory().getConnection().serverCommands().flushDb();
    connectionFactory.destroy();
  }

  @Test
  void atomicallyDecrementsAndRejectsInsufficientCachedStock() {
    UUID artworkId = UUID.randomUUID();
    redis.opsForValue().set(key(artworkId), "2");

    Reservation first = reservations.reserve(artworkId, 1);
    Reservation second = reservations.reserve(artworkId, 2);

    assertThat(first.allowed()).isTrue();
    assertThat(first.cached()).isTrue();
    assertThat(second.allowed()).isFalse();
    assertThat(redis.opsForValue().get(key(artworkId))).isEqualTo("1");
  }

  @Test
  void releasesCachedReservationAndRebuildsFromDatabaseStock() {
    UUID artworkId = UUID.randomUUID();
    redis.opsForValue().set(key(artworkId), "1");
    Reservation reservation = reservations.reserve(artworkId, 1);

    reservations.release(reservation);

    assertThat(redis.opsForValue().get(key(artworkId))).isEqualTo("1");

    InventoryStockReadPort stockReader = mock(InventoryStockReadPort.class);
    when(stockReader.findPublishedStock(any())).thenReturn(OptionalInt.of(7));
    reservations = new RedisStockReservationAdapter(redis, stockReader);

    assertThat(reservations.rebuild(artworkId)).isTrue();
    assertThat(redis.opsForValue().get(key(artworkId))).isEqualTo("7");
  }

  private static String key(UUID artworkId) {
    return "creatorhub:inventory:available:" + artworkId;
  }
}
