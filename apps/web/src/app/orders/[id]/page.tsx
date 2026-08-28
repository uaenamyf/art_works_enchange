"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";

interface OrderDetail { id: string; orderNumber: string; title: string; unitPrice: string; priceCurrency: string; quantity: number; subtotal: string; status: string; createdAt: string; expiresAt: string; fulfillmentType?: "PHYSICAL" | "DIGITAL"; shippingAddress?: { recipientName: string; line1: string; city: string; postalCode: string; countryCode: string; phone: string }; entitlement?: { status: "PENDING" | "AVAILABLE" | "REVOKED" | "PROCESSING"; fileName?: string; sizeBytes?: number; version?: number } | null }
interface DownloadGrant { grantId: string; fileName: string; contentType: string; sizeBytes: number; version: number; downloadUrl: string; expiresAt: string }

/** 订单详情页：展示服务端订单快照，不在前端推进行状态。 */
export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { session, loading, authenticatedFetch } = useAuth();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [download, setDownload] = useState<DownloadGrant | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!session) return;
    void params.then(({ id }) => authenticatedFetch<OrderDetail>(`/api/v1/orders/${id}`))
      .then(setOrder)
      .catch((requestError: unknown) => setError(requestError instanceof ApiError ? requestError.problem.detail ?? "订单加载失败" : "订单加载失败"));
  }, [authenticatedFetch, params, session]);

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录后查看订单。" />;
  if (error) return <Message text={error} alert />;
  if (!order) return <Message text="正在加载订单..." />;
  const loadedOrderId = order.id;
  async function requestDownload() {
    setDownloadError(null);
    setDownloading(true);
    try {
      const grant = await authenticatedFetch<DownloadGrant>(`/api/v1/orders/${loadedOrderId}/download-grants`, { method: "POST" });
      setDownload(grant);
      window.location.assign(grant.downloadUrl);
    } catch (requestError) {
      setDownloadError(requestError instanceof ApiError ? requestError.problem.detail ?? "下载授权已失效，请稍后重试" : "下载授权失败，请稍后重试");
    } finally {
      setDownloading(false);
    }
  }
  return (
    <main className="mx-auto w-full max-w-[960px] px-6 py-12">
      <Link href="/orders" className="text-sm text-ink-500 hover:text-ink-900">返回订单</Link>
      <header className="mt-6 border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.2em] text-brand-500">订单 {order.orderNumber}</p><h1 className="mt-3 text-4xl text-ink-900">{order.title}</h1><p className="mt-3 text-ink-700">状态：{order.status}</p></header>
      <div className="grid gap-8 py-8 md:grid-cols-2"><section><h2 className="text-xl text-ink-900">订单摘要</h2><dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between gap-4"><dt className="text-ink-500">数量</dt><dd>{order.quantity}</dd></div><div className="flex justify-between gap-4"><dt className="text-ink-500">单价</dt><dd>{order.unitPrice} {order.priceCurrency}</dd></div><div className="flex justify-between gap-4 border-t border-ink-300 pt-3"><dt className="text-ink-500">小计</dt><dd>{order.subtotal} {order.priceCurrency}</dd></div></dl></section>{order.fulfillmentType === "DIGITAL" ? <section><h2 className="text-xl text-ink-900">数字交付</h2><p className="mt-4 text-sm text-ink-500">授权状态：{order.entitlement?.status ?? "处理中"}</p>{order.entitlement?.fileName ? <p className="mt-2 text-sm text-ink-700">{order.entitlement.fileName} · 版本 {order.entitlement.version ?? "-"}</p> : null}{download ? <p className="mt-3 text-sm text-success">下载授权已生成，链接有效至 {new Date(download.expiresAt).toLocaleTimeString("zh-CN")}</p> : null}{downloadError ? <p role="alert" className="mt-3 text-sm text-error">{downloadError}</p> : null}{order.entitlement?.status === "AVAILABLE" ? <Button type="button" loading={downloading} className="mt-5" onClick={() => void requestDownload()}>获取下载链接</Button> : null}</section> : <section><h2 className="text-xl text-ink-900">收货信息</h2>{order.shippingAddress ? <address className="mt-4 whitespace-pre-line text-sm not-italic leading-7 text-ink-700">{order.shippingAddress.recipientName}{"\n"}{order.shippingAddress.line1}{"\n"}{order.shippingAddress.city} {order.shippingAddress.postalCode}{"\n"}{order.shippingAddress.countryCode} · {order.shippingAddress.phone}</address> : <p className="mt-4 text-sm text-ink-500">暂无收货信息。</p>}</section>}</div>
      {order.status === "PENDING_PAYMENT" ? <p className="border-t border-ink-300 pt-5 text-sm text-ink-500">订单已创建，支付功能将在支付渠道接入后开放。</p> : null}
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="mx-auto w-full max-w-[960px] px-6 py-16"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}