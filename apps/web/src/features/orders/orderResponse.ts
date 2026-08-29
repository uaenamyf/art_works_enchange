import type { components } from "@/lib/api/schema.gen";

type OrderDetail = components["schemas"]["OrderDetail"];
type OrderPage = components["schemas"]["OrderPage"];

/** 订单读取模型；兼容契约迁移期间后端仍返回的扁平订单快照。 */
export function normalizeOrder(value: unknown): OrderDetail {
  const order = value as Record<string, unknown>;
  if (Array.isArray(order.items) && order.total) return value as OrderDetail;

  const amount = String(order.subtotal ?? order.unitPrice ?? "0");
  const currency = String(order.currency ?? "CNY");
  const artworkId = String(order.artworkId ?? "00000000-0000-0000-0000-000000000000");
  return {
    id: String(order.id),
    orderNumber: String(order.orderNumber),
    buyerId: String(order.buyerId),
    seller: {
      id: "00000000-0000-0000-0000-000000000000",
      slug: "unknown",
      displayName: "创作者",
      category: "ART",
      verified: false,
      followerCount: 0,
      followedByMe: false
    },
    status: order.status as OrderDetail["status"],
    fulfillmentType: (order.fulfillmentType ?? "PHYSICAL") as OrderDetail["fulfillmentType"],
    items: [{
      artworkId,
      title: String(order.title ?? "订单商品"),
      coverUrl: "",
      unitPrice: { amount: String(order.unitPrice ?? amount), currency },
      quantity: Number(order.quantity ?? 1),
      subtotal: { amount, currency }
    }],
    subtotal: { amount, currency },
    shippingFee: { amount: "0", currency },
    total: { amount, currency },
    shippingAddress: (order.shippingAddress ?? null) as OrderDetail["shippingAddress"],
    trackingNumber: (order.trackingNumber ?? null) as string | null,
    carrier: (order.carrier ?? null) as string | null,
    expiresAt: (order.expiresAt ?? null) as string | null,
    createdAt: String(order.createdAt),
    updatedAt: String(order.updatedAt)
  };
}

/** 将订单列表的每一项统一为页面使用的 OpenAPI 订单模型。 */
export function normalizeOrderPage(value: unknown): OrderPage {
  const page = value as { items?: unknown[]; nextCursor?: string | null; hasMore?: boolean };
  return {
    items: (page.items ?? []).map(normalizeOrder),
    nextCursor: page.nextCursor ?? null,
    hasMore: page.hasMore ?? false
  };
}