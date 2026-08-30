"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { ROUTES } from "@/lib/config";
import { useAuth } from "@/features/auth/AuthProvider";
import { normalizeOrderPage } from "@/features/orders/orderResponse";
import type { components } from "@/lib/api/schema.gen";

type OrderPageResponse = components["schemas"]["OrderPage"];
type OrderStatus = components["schemas"]["OrderStatus"];

const statuses: Array<{ label: string; value?: OrderStatus }> = [
  { label: "全部" },
  { label: "待支付", value: "PENDING_PAYMENT" },
  { label: "已支付", value: "PAID" },
  { label: "处理中", value: "PROCESSING" },
  { label: "已发货", value: "SHIPPED" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" }
];

/** 买家订单历史：订单状态完全以服务端响应为准。 */
export default function OrdersPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const searchParams = useSearchParams();
  const requestedStatus = searchParams.get("status") ?? "";
  const status = statuses.find((item) => item.value === requestedStatus)?.value;
  const orders = useQuery<OrderPageResponse, Error>({
    queryKey: ["orders", "mine", status],
    enabled: Boolean(session) && !loading,
    queryFn: async () => {
      const params = new URLSearchParams({ limit: "20" });
      if (status) params.set("status", status);
      return normalizeOrderPage(
        await authenticatedFetch<unknown>(`/api/v1/orders?${params.toString()}`)
      );
    }
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录后查看订单。" action={<Link href={ROUTES.authLogin} className="mt-5 inline-flex h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">去登录</Link>} />;
  if (orders.isError) return <Message text={orders.error instanceof ApiError ? orders.error.problem.detail ?? "订单加载失败" : "订单加载失败"} alert />;
  if (orders.isPending) return <Message text="正在加载订单..." />;
  const result = orders.data;

  return (
    <main className="mx-auto w-full max-w-[960px] px-6 py-12">
      <header className="mb-8 border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.2em] text-brand-500">CreatorHub / 订单</p><h1 className="mt-3 text-4xl text-ink-900">我的订单</h1></header>
      <nav aria-label="订单状态筛选" className="mb-8 flex gap-2 overflow-x-auto border-b border-ink-300 pb-4">
        {statuses.map((item) => {
          const href = item.value ? `/orders?status=${item.value}` : "/orders";
          const selected = item.value === status || (!item.value && !status);
          return (
            <Link
              key={item.label}
              href={href}
              aria-current={selected ? "page" : undefined}
              className="min-h-11 shrink-0 rounded-token border border-ink-300 px-4 py-2 text-sm text-ink-700 hover:border-ink-900 hover:text-ink-900"
            >
              {item.label}
            </Link>
          );
        })}
      </nav>
      {result.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">还没有订单。</p> : <ul className="divide-y divide-ink-300 border-y border-ink-300">{result.items.map((order) => { const firstItem = order.items[0]; return <li key={order.id} className="py-5"><Link href={`/orders/${order.id}`} className="grid gap-2 sm:grid-cols-[1fr_auto] sm:items-center"><span><span className="block text-lg text-ink-900">{firstItem?.title ?? "订单商品"}</span><span className="mt-1 block text-sm text-ink-500">订单 {order.orderNumber} · {new Date(order.createdAt).toLocaleDateString("zh-CN")}</span></span><span className="text-sm text-ink-700">{order.status} · {order.total.amount} {order.total.currency}</span></Link></li>; })}</ul>}
    </main>
  );
}

function Message({ text, alert = false, action }: { text: string; alert?: boolean; action?: React.ReactNode }) {
  return <main className="mx-auto w-full max-w-[960px] px-6 py-16"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p>{action}</main>;
}