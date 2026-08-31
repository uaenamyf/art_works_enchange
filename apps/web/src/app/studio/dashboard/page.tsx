"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

type StudioDashboard = components["schemas"]["StudioDashboard"];

/** Creator Studio 概览；指标完全来自只读 Dashboard 接口，不在前端计算收入。 */
export default function StudioDashboardPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const dashboard = useQuery<StudioDashboard, Error>({
    queryKey: ["studio-dashboard"],
    enabled: Boolean(session) && !loading,
    queryFn: () => authenticatedFetch<StudioDashboard>("/api/v1/studio/dashboard")
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录并完成创作者申请。" alert />;
  if (dashboard.isError) return <Message text={dashboard.error instanceof ApiError ? dashboard.error.problem.detail ?? "概览加载失败" : "概览加载失败"} alert />;
  if (dashboard.isPending) return <Message text="正在加载概览..." />;

  const metrics = [
    ["累计收入", `${dashboard.data.revenue.amount} ${dashboard.data.revenue.currency}`],
    ["订单数", dashboard.data.orderCount],
    ["已发布作品", dashboard.data.publishedArtworkCount],
    ["关注者", dashboard.data.followerCount],
    ["数字下载", dashboard.data.digitalDownloadCount]
  ] as const;
  return (
    <main>
      <header className="border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Overview</p><h1 className="mt-2 font-serif text-4xl text-ink-900">工作台概览</h1><p className="mt-3 text-sm text-ink-500">查看作品、订单和创作者社区的最新状态。</p></header>
      <dl className="grid grid-cols-2 gap-x-6 gap-y-8 border-b border-ink-300 py-8 md:grid-cols-4">{metrics.map(([label, value]) => <div key={label}><dt className="text-sm text-ink-500">{label}</dt><dd className="mt-2 text-2xl text-ink-900">{value}</dd></div>)}</dl>
      <div className="flex flex-wrap gap-4 py-8"><Link href="/studio/artworks" className="inline-flex min-h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">管理作品</Link><Link href="/studio/orders" className="inline-flex min-h-11 items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900">查看订单</Link></div>
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}