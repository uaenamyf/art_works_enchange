package com.creatorhub.payment.application.service;

import com.creatorhub.creator.application.port.in.CreatorUseCases;
import com.creatorhub.ordering.domain.model.Order;
import com.creatorhub.ordering.domain.repository.OrderRepository;
import com.creatorhub.payment.application.port.in.PaymentUseCases;
import com.creatorhub.payment.application.port.out.PaymentProviderPort;
import com.creatorhub.payment.domain.model.PaymentProvider;
import com.creatorhub.payment.domain.model.PaymentSession;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 支付应用服务；只编排买家归属、Creator 收款账户和 Provider 调用，不推进订单支付状态。 */
@Service
@Transactional
public class PaymentService implements PaymentUseCases {

  private final OrderRepository orders;
  private final CreatorUseCases creators;
  private final List<PaymentProviderPort> providers;

  public PaymentService(
      OrderRepository orders, CreatorUseCases creators, List<PaymentProviderPort> providers) {
    this.orders = orders;
    this.creators = creators;
    this.providers = providers;
  }

  @Override
  public PaymentSession createSession(
      UUID buyerId, UUID orderId, PaymentProvider provider, String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new BusinessError("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key is required");
    }
    Order order = ownedOrder(buyerId, orderId);
    if (order.status() != com.creatorhub.ordering.domain.model.OrderStatus.PENDING_PAYMENT) {
      throw new BusinessError("PAYMENT_ORDER_INVALID", "Order is not awaiting payment");
    }
    PaymentProviderPort paymentProvider = provider(provider);
    if (order.sellerId() == null) {
      throw new BusinessError("PAYMENT_RECIPIENT_UNAVAILABLE", "Order recipient is unavailable");
    }
    String merchantId = creators.getPayPalAccount(order.sellerId().value()).merchantId();
    if (merchantId == null || merchantId.isBlank()) {
      throw new BusinessError(
          "PAYMENT_RECIPIENT_UNAVAILABLE", "Creator PayPal account is not configured");
    }
    return paymentProvider.createPayment(
        UUID.randomUUID(),
        order.id().value(),
        order.orderNumber(),
        order.subtotal(),
        idempotencyKey,
        merchantId);
  }

  @Override
  public PaymentSession captureSession(UUID buyerId, UUID orderId, String providerOrderId) {
    Order order = ownedOrder(buyerId, orderId);
    if (providerOrderId == null || providerOrderId.isBlank()) {
      throw new BusinessError("PAYMENT_PROVIDER_ORDER_REQUIRED", "PayPal order ID is required");
    }
    return provider(PaymentProvider.PAYPAL)
        .capturePayment(UUID.randomUUID(), order.id().value(), providerOrderId);
  }

  private Order ownedOrder(UUID buyerId, UUID orderId) {
    return orders
        .findById(orderId)
        .filter(order -> order.buyerId().value().equals(buyerId))
        .orElseThrow(() -> new BusinessError("ORDER_NOT_FOUND", "Order not found"));
  }

  private PaymentProviderPort provider(PaymentProvider provider) {
    return providers.stream()
        .filter(candidate -> candidate.provider() == provider)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessError(
                    "PAYMENT_PROVIDER_UNAVAILABLE", "Payment provider is unavailable"));
  }
}
