"use client";

import Link from "next/link";
import { use, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import { normalizeOrder } from "@/features/orders/orderResponse";
import { getOrderStatusCopy } from "@/features/orders/orderStatus";
import type { components } from "@/lib/api/schema.gen";
import { useLocale } from "@/features/locale/LocaleProvider";

type OrderDetail = components["schemas"]["OrderDetail"];
type DownloadGrant = components["schemas"]["DigitalDownloadGrant"];
type DigitalEntitlement = components["schemas"]["DigitalEntitlement"];

/** 订单详情页：展示服务端订单快照，不在前端推进行状态。 */
export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { session, loading, authenticatedFetch } = useAuth();
  const { locale, t } = useLocale();
  const { id } = use(params);
  const [download, setDownload] = useState<DownloadGrant | null>(null);
  const orderQuery = useQuery<OrderDetail, Error>({
    queryKey: ["orders", id],
    enabled: Boolean(session) && !loading,
    queryFn: async () => normalizeOrder(await authenticatedFetch<unknown>(`/api/v1/orders/${id}`))
  });
    const entitlementQuery = useQuery<DigitalEntitlement, Error>({
      queryKey: ["digital-entitlement", id],
      enabled: Boolean(session) && !loading && orderQuery.data?.fulfillmentType === "DIGITAL",
      queryFn: () => authenticatedFetch<DigitalEntitlement>(`/api/v1/orders/${id}/entitlement`)
    });
  const downloadMutation = useMutation<DownloadGrant, unknown, void>({
    mutationFn: () => authenticatedFetch<DownloadGrant>(`/api/v1/orders/${id}/download-grants`, { method: "POST" }),
    onSuccess: (grant) => {
      setDownload(grant);
      window.location.assign(grant.downloadUrl);
    }
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录后查看订单。" />;
  if (orderQuery.isError) return <Message text={orderQuery.error instanceof ApiError ? orderQuery.error.problem.detail ?? "订单加载失败" : "订单加载失败"} alert />;
  if (orderQuery.isPending) return <Message text="正在加载订单..." />;
  const order = orderQuery.data;
  const statusCopy = getOrderStatusCopy(order.status, locale);
  return (
    <main className="mx-auto w-full max-w-[960px] px-6 py-12">
      <Link href="/orders" className="text-sm text-ink-500 hover:text-ink-900">{t("backToOrders")}</Link>
      <header className="mt-6 border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.2em] text-brand-500">{t("order")} {order.orderNumber}</p><h1 className="mt-3 text-4xl text-ink-900">{order.items[0]?.title ?? (locale === "en-US" ? "Order details" : "订单详情")}</h1><p className="mt-3 text-ink-700">{locale === "en-US" ? "Status" : "状态"}：{order.status}</p></header>
      <section aria-label="订单结果" className="border-b border-ink-300 py-6">
        <p className={`text-lg ${statusCopy.tone === "success" ? "text-success" : statusCopy.tone === "warning" ? "text-error" : "text-ink-900"}`}>{statusCopy.title}</p>
        <p className="mt-2 text-sm text-ink-500">{statusCopy.detail}</p>
      </section>
      {order.fulfillmentType === "PHYSICAL" ? <section aria-label={t("logistics")} className="border-b border-ink-300 py-6"><h2 className="text-xl text-ink-900">{t("logistics")}</h2>{order.trackingNumber && order.carrier ? <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-3"><div><dt className="text-ink-500">{t("carrier")}</dt><dd className="mt-1 text-ink-900">{order.carrier}</dd></div><div><dt className="text-ink-500">{t("trackingNumber")}</dt><dd className="mt-1 break-all text-ink-900">{order.trackingNumber}</dd></div><div><dt className="text-ink-500">{t("deliveryStatus")}</dt><dd className="mt-1 text-ink-900">{order.status === "SHIPPED" || order.status === "COMPLETED" ? t("shippedAwaitingDelivery") : t("awaitingShipment")}</dd></div></dl> : <p className="mt-3 text-sm text-ink-500">{t("creatorWillAddTracking")}</p>}</section> : null}
      <div className="grid gap-8 py-8 md:grid-cols-2"><section><h2 className="text-xl text-ink-900">订单摘要</h2><dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between gap-4"><dt className="text-ink-500">数量</dt><dd>{order.items.reduce((total, item) => total + item.quantity, 0)}</dd></div><div className="flex justify-between gap-4"><dt className="text-ink-500">总计</dt><dd>{order.total.amount} {order.total.currency}</dd></div><div className="flex justify-between gap-4 border-t border-ink-300 pt-3"><dt className="text-ink-500">创建时间</dt><dd>{new Date(order.createdAt).toLocaleDateString("zh-CN")}</dd></div></dl></section>{order.fulfillmentType === "DIGITAL" ? <section><h2 className="text-xl text-ink-900">数字交付</h2>{entitlementQuery.isPending ? <p className="mt-4 text-sm text-ink-500">正在查询授权状态…</p> : entitlementQuery.isError ? <p role="alert" className="mt-4 text-sm text-error">{entitlementQuery.error instanceof ApiError ? entitlementQuery.error.problem.detail ?? "授权状态暂不可用" : "授权状态暂不可用"}</p> : <><p className="mt-4 text-sm text-ink-700">授权状态：{entitlementQuery.data.status}</p><p className="mt-2 text-sm text-ink-500">{entitlementQuery.data.status === "AVAILABLE" ? "支付已确认，可以申请短期下载链接。" : entitlementQuery.data.status === "REVOKED" ? "该资产已撤回，暂时无法下载。" : "支付确认后将生成下载授权。"}</p></>}{download ? <p className="mt-3 text-sm text-success">下载授权已生成，链接有效至 {new Date(download.expiresAt).toLocaleTimeString("zh-CN")}</p> : null}{downloadMutation.isError ? <p role="alert" className="mt-3 text-sm text-error">{downloadMutation.error instanceof ApiError ? downloadMutation.error.problem.detail ?? "下载授权已失效，请稍后重试" : "下载授权失败，请稍后重试"}</p> : null}<Button type="button" loading={downloadMutation.isPending} className="mt-5" onClick={() => downloadMutation.mutate()} disabled={downloadMutation.isPending || order.status !== "PAID" || entitlementQuery.data?.status !== "AVAILABLE"}>获取下载链接</Button></section> : <section><h2 className="text-xl text-ink-900">收货信息</h2>{order.shippingAddress ? <address className="mt-4 whitespace-pre-line text-sm not-italic leading-7 text-ink-700">{order.shippingAddress.recipientName}{"\n"}{order.shippingAddress.line1}{"\n"}{order.shippingAddress.city} {order.shippingAddress.postalCode}{"\n"}{order.shippingAddress.countryCode} · {order.shippingAddress.phone}</address> : <p className="mt-4 text-sm text-ink-500">暂无收货信息。</p>}{order.trackingNumber || order.carrier ? <p className="mt-4 border-t border-ink-300 pt-4 text-sm text-ink-700">物流：{order.carrier ?? "承运商待更新"} · {order.trackingNumber ?? "运单号待更新"}</p> : null}</section>}</div>
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="mx-auto w-full max-w-[960px] px-6 py-16"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}