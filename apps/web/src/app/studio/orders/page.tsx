"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import { normalizeOrderPage } from "@/features/orders/orderResponse";
import type { components } from "@/lib/api/schema.gen";
import { useLocale } from "@/features/locale/LocaleProvider";

type OrderPage = components["schemas"]["OrderPage"];
type OrderStatus = components["schemas"]["OrderStatus"];
type UpdateOrderStatusRequest = components["schemas"]["UpdateOrderStatusRequest"];

const statuses: Array<{ label: string; value?: OrderStatus }> = [
  { label: "全部" },
  { label: "待支付", value: "PENDING_PAYMENT" },
  { label: "已支付", value: "PAID" },
  { label: "处理中", value: "PROCESSING" },
  { label: "已发货", value: "SHIPPED" },
  { label: "已完成", value: "COMPLETED" }
];

/** Studio 销售订单列表；状态推进交给后端订单状态机和对象级权限校验。 */
export default function StudioOrdersPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const { locale, t } = useLocale();
  const searchParams = typeof window === "undefined" ? new URLSearchParams() : new URLSearchParams(window.location.search);
  const query = searchParams.get("status") ?? "";
  const cursor = searchParams.get("cursor");
  const status = statuses.find((item) => item.value === query)?.value;
  const queryClient = useQueryClient();
  const [shippingDraft, setShippingDraft] = useState<Record<string, { carrier: string; trackingNumber: string }>>({});
  const orders = useQuery<OrderPage, Error>({
    queryKey: ["studio-orders", { status, cursor }],
    enabled: Boolean(session) && !loading,
    queryFn: async () => {
      const params = new URLSearchParams({ limit: "20" });
      if (status) params.set("status", status);
      if (cursor) params.set("cursor", cursor);
      return normalizeOrderPage(await authenticatedFetch<unknown>(`/api/v1/studio/orders?${params.toString()}`));
    }
  });
  const updateStatus = useMutation<unknown, Error, { orderId: string; request: UpdateOrderStatusRequest }>({
    mutationFn: ({ orderId, request }) => authenticatedFetch(`/api/v1/studio/orders/${orderId}/status`, { method: "PATCH", body: request }),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["studio-orders"] }); }
  });

  if (loading) return <Message text={t("loadingAccount")} />;
  if (!session) return <Message text={locale === "en-US" ? "Log in and become a creator to use Studio." : "请先登录并完成创作者申请。"} alert />;
  if (orders.isError) return <Message text={locale === "en-US" ? t("ordersLoadFailed") : orders.error instanceof ApiError ? orders.error.problem.detail ?? t("ordersLoadFailed") : t("ordersLoadFailed")} alert />;
  if (orders.isPending) return <Message text={t("loadingOrders")} />;

  return (
    <main>
      <header className="border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Orders</p><h1 className="mt-2 font-serif text-4xl text-ink-900">{t("fulfillmentTitle")}</h1><p className="mt-3 text-sm text-ink-500">{t("fulfillmentHint")}</p></header>
      <nav aria-label="订单状态筛选" className="flex gap-2 overflow-x-auto border-b border-ink-300 py-4">{statuses.map((item) => <Link key={item.label} href={item.value ? `/studio/orders?status=${item.value}` : "/studio/orders"} aria-current={item.value === status || (!item.value && !status) ? "page" : undefined} className="min-h-11 shrink-0 rounded-token border border-ink-300 px-4 py-2 text-sm text-ink-700 hover:border-ink-900 hover:text-ink-900">{item.label}</Link>)}</nav>
      {orders.data.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">{t("noOrdersInFilter")}</p> : <ul className="divide-y divide-ink-300 border-b border-ink-300">{orders.data.items.map((order) => { const draft = shippingDraft[order.id] ?? { carrier: order.carrier ?? "", trackingNumber: order.trackingNumber ?? "" }; return <li key={order.id} className="py-5"><div className="flex flex-col gap-4"><div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"><Link href={`/orders/${order.id}`} className="min-w-0"><span className="block text-lg text-ink-900">{order.items[0]?.title ?? (locale === "en-US" ? "Order item" : "订单商品")}</span><span className="mt-1 block text-sm text-ink-500">{locale === "en-US" ? "Order" : "订单"} {order.orderNumber} · {locale === "en-US" ? "Buyer" : "买家"} {order.buyerId.slice(0, 8)} · {order.total.amount} {order.total.currency}</span></Link><div className="flex shrink-0 items-center gap-3"><span className="text-sm text-ink-700">{order.status}</span>{order.status === "PAID" ? <button type="button" className="min-h-11 rounded-token border border-ink-900 px-4 text-sm text-ink-900" disabled={updateStatus.isPending} onClick={() => updateStatus.mutate({ orderId: order.id, request: { status: "PROCESSING" } })}>{t("acceptOrder")}</button> : null}</div></div>{order.status === "PROCESSING" ? <div className="grid gap-3 sm:grid-cols-[1fr_1fr_auto]"><input aria-label={`${order.orderNumber} ${t("carrier")}`} value={draft.carrier} onChange={(event) => setShippingDraft({ ...shippingDraft, [order.id]: { ...draft, carrier: event.target.value } })} placeholder={t("carrierPlaceholder")} className="min-h-11 rounded-token border border-ink-300 px-3 text-sm" /><input aria-label={`${order.orderNumber} ${t("trackingNumber")}`} value={draft.trackingNumber} onChange={(event) => setShippingDraft({ ...shippingDraft, [order.id]: { ...draft, trackingNumber: event.target.value } })} placeholder={t("trackingPlaceholder")} className="min-h-11 rounded-token border border-ink-300 px-3 text-sm" /><button type="button" className="min-h-11 rounded-token border border-ink-900 px-4 text-sm text-ink-900" disabled={updateStatus.isPending || !draft.carrier.trim() || !draft.trackingNumber.trim()} onClick={() => updateStatus.mutate({ orderId: order.id, request: { status: "SHIPPED", carrier: draft.carrier.trim(), trackingNumber: draft.trackingNumber.trim() } })}>{t("markShipped")}</button></div> : null}</div></li>; })}</ul>}
      {updateStatus.isError ? <p role="alert" className="mt-4 text-sm text-error">{updateStatus.error instanceof ApiError ? updateStatus.error.problem.detail ?? "订单状态更新失败" : "订单状态更新失败"}</p> : null}
      {orders.data.hasMore && orders.data.nextCursor ? <Link href={`/studio/orders?status=${status ?? ""}&cursor=${encodeURIComponent(orders.data.nextCursor)}`} className="mt-6 inline-flex min-h-11 items-center border-b border-ink-900 text-sm text-ink-900">加载更多</Link> : null}
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}