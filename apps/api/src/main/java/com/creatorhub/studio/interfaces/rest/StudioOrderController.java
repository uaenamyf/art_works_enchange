package com.creatorhub.studio.interfaces.rest;

import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderPage;
import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderView;
import com.creatorhub.ordering.domain.model.OrderStatus;
import com.creatorhub.studio.application.port.in.StudioOrderUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Studio 订单 REST 适配器；只读取和推进当前 Creator 负责的订单。 */
@RestController
@RequestMapping("/api/v1/studio/orders")
public class StudioOrderController {

  private final StudioOrderUseCase orders;

  public StudioOrderController(StudioOrderUseCase orders) {
    this.orders = orders;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
  ResponseEntity<OrderPage> list(
      @AuthenticationPrincipal UUID userId,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(orders.list(userId, status, cursor, limit));
  }

  @PatchMapping("/{orderId}/status")
  @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
  ResponseEntity<OrderView> updateStatus(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID orderId,
      @Valid @RequestBody UpdateStatusRequest request) {
    return ResponseEntity.ok(
        orders.updateStatus(
            userId, orderId, request.status, request.trackingNumber, request.carrier));
  }

  record UpdateStatusRequest(
      OrderStatus status, @Size(max = 100) String trackingNumber, @Size(max = 80) String carrier) {}
}
