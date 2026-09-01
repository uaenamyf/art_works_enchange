package com.creatorhub.ordering.interfaces.rest;

import com.creatorhub.ordering.application.port.in.OrderUseCases;
import com.creatorhub.ordering.application.port.in.OrderUseCases.CreateCommand;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.ordering.domain.model.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 订单 HTTP 适配器；不接受客户端金额，幂等键从请求头读取。 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderUseCases orders;

  public OrderController(OrderUseCases orders) {
    this.orders = orders;
  }

  /** 创建待支付订单；订单成功后才返回 201 和订单资源地址。 */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<OrderUseCases.OrderView> create(
      @AuthenticationPrincipal UUID buyerId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody CreateOrderRequest request) {
    OrderUseCases.OrderView order = orders.create(buyerId, request.command(), idempotencyKey);
    return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).body(order);
  }

  /** 取消当前买家的待支付订单；重复取消返回当前已取消状态且不会重复释放库存。 */
  @PostMapping("/{orderId}/cancel")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<OrderUseCases.OrderView> cancel(
      @AuthenticationPrincipal UUID buyerId, @PathVariable UUID orderId) {
    return ResponseEntity.ok(orders.cancel(buyerId, orderId));
  }

  /** 返回当前买家的订单列表，状态筛选和游标均在服务端执行。 */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<OrderUseCases.OrderPage> list(
      @AuthenticationPrincipal UUID buyerId,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(orders.list(buyerId, status, cursor, limit));
  }

  /** 只允许买家读取自己的订单，其他用户统一按不存在处理。 */
  @GetMapping("/{orderId}")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<OrderUseCases.OrderView> get(
      @AuthenticationPrincipal UUID buyerId, @PathVariable UUID orderId) {
    return ResponseEntity.ok(orders.get(buyerId, orderId));
  }

  /** 只允许买家查询自己的本地物流信息，第三方轨迹后续由基础设施适配器接入。 */
  @GetMapping("/{orderId}/tracking")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<OrderUseCases.OrderTrackingView> getTracking(
      @AuthenticationPrincipal UUID buyerId, @PathVariable UUID orderId) {
    return ResponseEntity.ok(orders.getTracking(buyerId, orderId));
  }

  record CreateOrderRequest(
      @NotNull UUID artworkId, @Min(1) @Max(99) int quantity, @Valid AddressInput shippingAddress) {

    CreateCommand command() {
      return new CreateCommand(
          artworkId, quantity, shippingAddress == null ? null : shippingAddress.toDomain());
    }
  }

  record AddressInput(
      String recipientName,
      String line1,
      String line2,
      String city,
      String region,
      String postalCode,
      String countryCode,
      String phone) {

    ShippingAddress toDomain() {
      return new ShippingAddress(
          recipientName, line1, line2, city, region, postalCode, countryCode, phone);
    }
  }
}
