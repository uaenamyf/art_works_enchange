import { describe, expect, it } from "vitest";
import { normalizeOrder, normalizeOrderPage } from "./orderResponse";

describe("order response normalization", () => {
  it("converts the current flat order response to the generated order model", () => {
    const order = normalizeOrder({
      id: "order-1",
      orderNumber: "CH-1",
      buyerId: "buyer-1",
      artworkId: "artwork-1",
      title: "Vase",
      unitPrice: 188,
      currency: "CNY",
      quantity: 1,
      subtotal: 188,
      status: "PENDING_PAYMENT",
      fulfillmentType: "PHYSICAL",
      shippingAddress: null,
      createdAt: "2026-08-30T00:00:00Z",
      updatedAt: "2026-08-30T00:00:00Z",
      expiresAt: "2026-08-30T00:30:00Z"
    });

    expect(order.items[0]?.title).toBe("Vase");
    expect(order.total).toEqual({ amount: "188", currency: "CNY" });
    expect(order.items[0]?.quantity).toBe(1);
  });

  it("preserves an already normalized order page", () => {
    const page = normalizeOrderPage({ items: [], nextCursor: null, hasMore: false });

    expect(page).toEqual({ items: [], nextCursor: null, hasMore: false });
  });
});