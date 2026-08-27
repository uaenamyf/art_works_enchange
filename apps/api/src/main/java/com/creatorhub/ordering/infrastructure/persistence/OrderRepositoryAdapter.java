package com.creatorhub.ordering.infrastructure.persistence;

import com.creatorhub.catalog.domain.model.ArtworkId;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.model.OrderLine;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.Money;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** 订单仓储适配器，显式转换订单聚合和 PostgreSQL 持久化行。 */
@Repository
public class OrderRepositoryAdapter implements OrderRepository {

  private final OrderMapper mapper;
  private final ObjectMapper objectMapper;

  public OrderRepositoryAdapter(OrderMapper mapper, ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.objectMapper = objectMapper;
  }

  @Override
  public Order save(Order order) {
    OrderPersistenceRow row = new OrderPersistenceRow();
    row.setId(order.id().value());
    row.setOrderNumber(order.orderNumber());
    row.setBuyerId(order.buyerId().value());
    OrderLine line = order.lines().get(0);
    row.setArtworkId(line.artworkId().value());
    row.setTitle(line.title());
    row.setUnitPrice(line.unitPrice().amount());
    row.setCurrency(line.unitPrice().currency().getCurrencyCode());
    row.setQuantity(line.quantity());
    row.setSubtotal(line.subtotal().amount());
    row.setStatus(order.status());
    try {
      row.setShippingAddress(objectMapper.writeValueAsString(order.shippingAddress()));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Shipping address serialization failed", exception);
    }
    row.setExpiresAt(order.expiresAt());
    row.setCreatedAt(order.createdAt());
    row.setUpdatedAt(order.updatedAt());
    if (mapper.findByIdValue(row.getId()) == null) {
      mapper.insertOrder(row);
    } else if (mapper.cancel(row) != 1) {
      throw new IllegalStateException("Order update conflict");
    }
    return order;
  }

  @Override
  public Optional<Order> findById(UUID orderId) {
    return Optional.ofNullable(mapper.findByIdValue(orderId)).map(this::toDomain);
  }

  @Override
  public boolean cancel(Order order) {
    OrderPersistenceRow row = new OrderPersistenceRow();
    row.setId(order.id().value());
    row.setStatus(order.status());
    row.setUpdatedAt(order.updatedAt());
    return mapper.cancel(row) == 1;
  }

  @Override
  public List<Order> findExpiredPending(Instant now, int limit) {
    return mapper.findExpiredPending(now, limit).stream().map(this::toDomain).toList();
  }

  @Override
  public List<Order> findByBuyer(
      UUID buyerId, OrderStatus status, Instant beforeCreatedAt, UUID beforeId, int limit) {
    return mapper.findByBuyer(buyerId, status, beforeCreatedAt, beforeId, limit).stream()
        .map(this::toDomain)
        .toList();
  }

  private Order toDomain(OrderPersistenceRow row) {
    try {
      ShippingAddress address =
          objectMapper.readValue(row.getShippingAddress(), ShippingAddress.class);
      return Order.reconstitute(
          com.creatorhub.ordering.domain.model.OrderId.of(row.getId()),
          UserId.of(row.getBuyerId()),
          row.getOrderNumber(),
          new OrderLine(
              ArtworkId.of(row.getArtworkId()),
              row.getTitle(),
              Money.of(row.getUnitPrice().toPlainString(), row.getCurrency()),
              row.getQuantity()),
          address,
          row.getCreatedAt(),
          row.getExpiresAt(),
          row.getStatus(),
          row.getUpdatedAt());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Shipping address deserialization failed", exception);
    }
  }
}
