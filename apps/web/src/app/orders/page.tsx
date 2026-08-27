"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api/fetcher";
import { ROUTES } from "@/lib/config";
import { useAuth } from "@/features/auth/AuthProvider";

interface OrderItem {
  id: string;
  orderNumber: string;
  artworkId: string;
  title: string;
  unitPrice: string;
  priceCurrency: string;
  quantity: number;
  subtotal: string;
  status: string;
  createdAt: string;
}

interface OrderPageResponse { items: OrderItem[]; nextCursor: string | null; hasMore: boolean }

/** 买家订单历史：订单状态完全以服务端响应为准。 */
export default function OrdersPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const [result, setResult] = useState<OrderPageResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    void authenticatedFetch<OrderPageResponse>("/api/v1/orders?limit=20")
      .then(setResult)
      .catch((requestError: unknown) => setError(requestError instanceof ApiError ? requestError.problem.detail ?? "订单加载失败" : "订单加载失败"));
  }, [authenticatedFetch, session]);

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录后查看订单。" action={<Link href={ROUTES.authLogin} className="mt-5 inline-flex h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">去登录</Link>} />;
  if (error) return <Message text={error} alert />;
  if (!result) return <Message text="正在加载订单..." />;

  return (
    <main className="mx-auto w-full max-w-[960px] px-6 py-12">
      <header className="mb-8 border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.2em] text-brand-500">CreatorHub / 订单</p><h1 className="mt-3 text-4xl text-ink-900">我的订单</h1></header>
      {result.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">还没有订单。</p> : <ul className="divide-y divide-ink-300 border-y border-ink-300">{result.items.map((order) => <li key={order.id} className="py-5"><Link href={`/orders/${order.id}`} className="grid gap-2 sm:grid-cols-[1fr_auto] sm:items-center"><span><span className="block text-lg text-ink-900">{order.title}</span><span className="mt-1 block text-sm text-ink-500">订单 {order.orderNumber} · {new Date(order.createdAt).toLocaleDateString("zh-CN")}</span></span><span className="text-sm text-ink-700">{order.status} · {order.subtotal} {order.priceCurrency}</span></Link></li>)}</ul>}
    </main>
  );
}

function Message({ text, alert = false, action }: { text: string; alert?: boolean; action?: React.ReactNode }) {
  return <main className="mx-auto w-full max-w-[960px] px-6 py-16"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p>{action}</main>;
}