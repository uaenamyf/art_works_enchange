package com.creatorhub.ordering.domain.model;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.sharedkernel.domain.Money;
import java.util.Objects;

/** 不可变订单项价格快照；金额由 Catalog 价格产生，不能信任客户端传入。 */
public record OrderLine(ArtworkId artworkId, String title, Money unitPrice, int quantity) {

  public OrderLine {
    artworkId = Objects.requireNonNull(artworkId, "artworkId");
    title = Objects.requireNonNull(title, "title").trim();
    if (title.isEmpty()) {
      throw new IllegalArgumentException("title is invalid");
    }
    unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
    if (!unitPrice.isPositive()) {
      throw new IllegalArgumentException("unitPrice must be positive");
    }
    if (quantity < 1 || quantity > 99) {
      throw new IllegalArgumentException("quantity is invalid");
    }
  }

  public Money subtotal() {
    Money subtotal = Money.zero(unitPrice.currency());
    for (int index = 0; index < quantity; index++) {
      subtotal = subtotal.plus(unitPrice);
    }
    return subtotal;
  }
}
