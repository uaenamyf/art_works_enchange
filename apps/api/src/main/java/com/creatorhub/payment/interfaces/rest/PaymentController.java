package com.creatorhub.payment.interfaces.rest;

import com.creatorhub.payment.application.port.in.PaymentUseCases;
import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import com.creatorhub.sharedkernel.error.BusinessError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 支付 HTTP 适配器；买家只能为自己的待支付订单创建或捕获 Provider 会话。 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/payment")
public class PaymentController {

  private final PaymentUseCases payments;
  private static final boolean PURCHASES_ENABLED = false;

  public PaymentController(PaymentUseCases payments) {
    this.payments = payments;
  }

  @PostMapping("/session")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<PaymentSession> createSession(
      @AuthenticationPrincipal UUID buyerId,
      @PathVariable UUID orderId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody CreateSessionRequest request) {
    if (!PURCHASES_ENABLED) {
      throw purchasesDisabled();
    }
    return ResponseEntity.ok(
        payments.createSession(buyerId, orderId, request.provider(), idempotencyKey));
  }

  @PostMapping("/capture")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<PaymentSession> capture(
      @AuthenticationPrincipal UUID buyerId,
      @PathVariable UUID orderId,
      @Valid @RequestBody CaptureRequest request) {
    if (!PURCHASES_ENABLED) {
      throw purchasesDisabled();
    }
    return ResponseEntity.ok(payments.captureSession(buyerId, orderId, request.providerOrderId()));
  }

  private static BusinessError purchasesDisabled() {
    return new BusinessError(
        "PURCHASES_DISABLED", "Purchases are temporarily unavailable until payment is enabled");
  }

  record CreateSessionRequest(@NotNull PaymentProvider provider) {}

  record CaptureRequest(@NotBlank String providerOrderId) {}
}
