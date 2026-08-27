package com.creatorhub.ordering.domain.model;

import com.creatorhub.sharedkernel.domain.StrongId;
import java.util.UUID;

/** 订单聚合强类型 ID，防止订单 ID 与用户、作品 ID 混用。 */
public final class OrderId extends StrongId {

  private OrderId(UUID value) {
    super(value);
  }

  public static OrderId of(UUID value) {
    return new OrderId(value);
  }

  public static OrderId random() {
    return new OrderId(StrongId.randomUuid());
  }
}
