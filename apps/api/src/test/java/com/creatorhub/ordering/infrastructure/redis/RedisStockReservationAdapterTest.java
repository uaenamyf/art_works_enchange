package com.creatorhub.ordering.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 验证 Redis 库存闸门的原子结果和 Redis 故障降级语义。 */
class RedisStockReservationAdapterTest {

  @Test
  void allowsReservationWhenRedisCacheIsMissing() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    when(redis.execute(any(), any(), any())).thenReturn(1L);

    var result = new RedisStockReservationAdapter(redis).reserve(UUID.randomUUID(), 2);

    assertThat(result.allowed()).isTrue();
    assertThat(result.cached()).isFalse();
  }

  @Test
  void rejectsReservationWhenRedisCacheHasInsufficientStock() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    when(redis.execute(any(), any(), any())).thenReturn(0L);

    var result = new RedisStockReservationAdapter(redis).reserve(UUID.randomUUID(), 2);

    assertThat(result.allowed()).isFalse();
    assertThat(result.cached()).isTrue();
  }

  @Test
  void fallsBackToDatabaseWhenRedisIsUnavailable() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    when(redis.execute(any(), any(), any())).thenThrow(new IllegalStateException("redis down"));

    var result = new RedisStockReservationAdapter(redis).reserve(UUID.randomUUID(), 2);

    assertThat(result.allowed()).isTrue();
    assertThat(result.cached()).isFalse();
  }
}
