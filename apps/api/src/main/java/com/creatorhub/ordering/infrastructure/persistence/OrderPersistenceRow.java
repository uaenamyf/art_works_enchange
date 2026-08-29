package com.creatorhub.ordering.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.creatorhub.ordering.domain.model.FulfillmentType;
import com.creatorhub.ordering.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 订单写模型持久化行，仅供 Ordering infrastructure 使用。 */
@TableName("orders")
class OrderPersistenceRow {

  @TableId private UUID id;
  private String orderNumber;
  private UUID buyerId;
  private UUID sellerId;
  private UUID artworkId;
  private String title;
  private BigDecimal unitPrice;
  private String currency;
  private int quantity;
  private BigDecimal subtotal;
  private OrderStatus status;
  private FulfillmentType fulfillmentType;
  private String shippingAddress;
  private Instant expiresAt;
  private Instant createdAt;
  private Instant updatedAt;
  private String trackingNumber;
  private String carrier;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public UUID getBuyerId() {
    return buyerId;
  }

  public UUID getSellerId() {
    return sellerId;
  }

  public void setSellerId(UUID sellerId) {
    this.sellerId = sellerId;
  }

  public void setBuyerId(UUID buyerId) {
    this.buyerId = buyerId;
  }

  public UUID getArtworkId() {
    return artworkId;
  }

  public void setArtworkId(UUID artworkId) {
    this.artworkId = artworkId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public FulfillmentType getFulfillmentType() {
    return fulfillmentType;
  }

  public void setFulfillmentType(FulfillmentType fulfillmentType) {
    this.fulfillmentType = fulfillmentType;
  }

  public String getShippingAddress() {
    return shippingAddress;
  }

  public void setShippingAddress(String shippingAddress) {
    this.shippingAddress = shippingAddress;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }

  public String getCarrier() {
    return carrier;
  }

  public void setCarrier(String carrier) {
    this.carrier = carrier;
  }
}
