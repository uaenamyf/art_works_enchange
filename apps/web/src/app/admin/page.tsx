"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

type Dashboard = components["schemas"]["AdminDashboard"];

/** Admin 运营概览；只展示服务端聚合指标，不在前端推导交易金额。 */
export default function AdminDashboardPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const dashboard = useQuery<Dashboard, Error>({
    queryKey: ["admin-dashboard"],
    enabled: !loading && session?.user.role === "ADMIN",
    queryFn: () => authenticatedFetch<Dashboard>("/api/v1/admin/dashboard")
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session || session.user.role !== "ADMIN") return <Message text="只有管理员可以访问此页面。" alert />;
  if (dashboard.isError) return <Message text={dashboard.error instanceof ApiError ? dashboard.error.problem.detail ?? "概览加载失败" : "概览加载失败"} alert />;
  if (dashboard.isPending) return <Message text="正在加载运营指标..." />;

  const metrics = [
    ["用户总数", dashboard.data.userCount],
    ["Creator 总数", dashboard.data.creatorCount],
    ["已发布作品", dashboard.data.publishedArtworkCount],
    ["待审核作品", dashboard.data.pendingReviewArtworkCount]
  ] as const;
  return (
    <main>
      <header className="border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Admin / Overview</p><h1 className="mt-2 font-serif text-4xl text-ink-900">运营概览</h1><p className="mt-3 text-sm text-ink-500">查看平台内容和订单状态，不包含支付结算操作。</p></header>
      <dl className="grid grid-cols-2 gap-x-6 gap-y-8 border-b border-ink-300 py-8 md:grid-cols-4">{metrics.map(([label, value]) => <div key={label}><dt className="text-sm text-ink-500">{label}</dt><dd className="mt-2 text-2xl text-ink-900">{value}</dd></div>)}</dl>
      <div className="grid gap-8 py-8 md:grid-cols-2">
        <StatusList title="作品状态" values={dashboard.data.artworkCountByStatus} />
        <StatusList title="订单状态" values={dashboard.data.orderCountByStatus} />
      </div>
      <Link href="/admin/artworks/review" className="inline-flex min-h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">查看审核队列</Link>
    </main>
  );
}

function StatusList({ title, values }: { title: string; values: Record<string, number> }) {
  return <section className="border-b border-ink-300 pb-5"><h2 className="text-xl text-ink-900">{title}</h2><dl className="mt-4 divide-y divide-ink-300">{Object.entries(values).map(([status, count]) => <div key={status} className="flex justify-between py-3 text-sm"><dt className="text-ink-500">{status}</dt><dd className="text-ink-900">{count}</dd></div>)}</dl></section>;
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}
