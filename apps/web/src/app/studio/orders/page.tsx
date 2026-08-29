"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

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
  const searchParams = typeof window === "undefined" ? new URLSearchParams() : new URLSearchParams(window.location.search);
  const query = searchParams.get("status") ?? "";
  const cursor = searchParams.get("cursor");
  const status = statuses.find((item) => item.value === query)?.value;
  const queryClient = useQueryClient();
  const orders = useQuery<OrderPage, Error>({
    queryKey: ["studio-orders", { status, cursor }],
    enabled: Boolean(session) && !loading,
    queryFn: () => {
      const params = new URLSearchParams({ limit: "20" });
      if (status) params.set("status", status);
      if (cursor) params.set("cursor", cursor);
      return authenticatedFetch<OrderPage>(`/api/v1/studio/orders?${params.toString()}`);
    }
  });
  const updateStatus = useMutation<unknown, Error, { orderId: string; request: UpdateOrderStatusRequest }>({
    mutationFn: ({ orderId, request }) => authenticatedFetch(`/api/v1/studio/orders/${orderId}/status`, { method: "PATCH", body: request }),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["studio-orders"] }); }
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录并完成创作者申请。" alert />;
  if (orders.isError) return <Message text={orders.error instanceof ApiError ? orders.error.problem.detail ?? "订单加载失败" : "订单加载失败"} alert />;
  if (orders.isPending) return <Message text="正在加载订单..." />;

  return (
    <main>
      <header className="border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Orders</p><h1 className="mt-2 font-serif text-4xl text-ink-900">订单履约</h1><p className="mt-3 text-sm text-ink-500">处理已支付订单并推进履约状态。</p></header>
      <nav aria-label="订单状态筛选" className="flex gap-2 overflow-x-auto border-b border-ink-300 py-4">{statuses.map((item) => <Link key={item.label} href={item.value ? `/studio/orders?status=${item.value}` : "/studio/orders"} aria-current={item.value === status || (!item.value && !status) ? "page" : undefined} className="min-h-11 shrink-0 rounded-token border border-ink-300 px-4 py-2 text-sm text-ink-700 hover:border-ink-900 hover:text-ink-900">{item.label}</Link>)}</nav>
      {orders.data.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">当前筛选下暂无订单。</p> : <ul className="divide-y divide-ink-300 border-b border-ink-300">{orders.data.items.map((order) => <li key={order.id} className="py-5"><div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"><Link href={`/orders/${order.id}`} className="min-w-0"><span className="block text-lg text-ink-900">{order.items[0]?.title ?? "订单商品"}</span><span className="mt-1 block text-sm text-ink-500">订单 {order.orderNumber} · 买家 {order.buyerId.slice(0, 8)} · {order.total.amount} {order.total.currency}</span></Link><div className="flex shrink-0 items-center gap-3"><span className="text-sm text-ink-700">{order.status}</span>{order.status === "PAID" ? <button type="button" className="min-h-11 rounded-token border border-ink-900 px-4 text-sm text-ink-900" disabled={updateStatus.isPending} onClick={() => updateStatus.mutate({ orderId: order.id, request: { status: "PROCESSING" } })}>接单</button> : null}{order.status === "PROCESSING" ? <button type="button" className="min-h-11 rounded-token border border-ink-900 px-4 text-sm text-ink-900" disabled={updateStatus.isPending} onClick={() => updateStatus.mutate({ orderId: order.id, request: { status: "SHIPPED" } })}>标记发货</button> : null}</div></div></li>)}</ul>}
      {updateStatus.isError ? <p role="alert" className="mt-4 text-sm text-error">{updateStatus.error instanceof ApiError ? updateStatus.error.problem.detail ?? "订单状态更新失败" : "订单状态更新失败"}</p> : null}
      {orders.data.hasMore && orders.data.nextCursor ? <Link href={`/studio/orders?status=${status ?? ""}&cursor=${encodeURIComponent(orders.data.nextCursor)}`} className="mt-6 inline-flex min-h-11 items-center border-b border-ink-900 text-sm text-ink-900">加载更多</Link> : null}
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}