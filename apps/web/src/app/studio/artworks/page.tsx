"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

type ArtworkPage = components["schemas"]["StudioArtworkPage"];
type ArtworkStatus = components["schemas"]["ArtworkStatus"];

const statuses: Array<{ label: string; value?: ArtworkStatus }> = [
  { label: "全部" },
  { label: "草稿", value: "DRAFT" },
  { label: "待审核", value: "PENDING_REVIEW" },
  { label: "已发布", value: "PUBLISHED" },
  { label: "已归档", value: "ARCHIVED" }
];

/** Creator Studio 作品列表；筛选状态保存在 URL，列表数据由 Query 缓存。 */
export default function StudioArtworksPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const searchParams = typeof window === "undefined" ? new URLSearchParams() : new URLSearchParams(window.location.search);
  const query = searchParams.get("status") ?? "";
  const cursor = searchParams.get("cursor");
  const status = statuses.find((item) => item.value === query)?.value;
  const artworks = useQuery<ArtworkPage, Error>({
    queryKey: ["studio-artworks", { status }],
    enabled: Boolean(session) && !loading,
    queryFn: () => {
      const params = new URLSearchParams({ limit: "20" });
      if (status) params.set("status", status);
      if (cursor) params.set("cursor", cursor);
      return authenticatedFetch<ArtworkPage>(`/api/v1/studio/artworks?${params.toString()}`);
    }
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录并完成创作者申请。" alert />;
  if (artworks.isError) return <Message text={artworks.error instanceof ApiError ? artworks.error.problem.detail ?? "作品加载失败" : "作品加载失败"} alert />;
  if (artworks.isPending) return <Message text="正在加载作品..." />;

  return (
    <main>
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-ink-300 pb-6">
        <div><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Works</p><h1 className="mt-2 font-serif text-4xl text-ink-900">作品管理</h1><p className="mt-3 text-sm text-ink-500">管理草稿、审核和已发布作品。</p></div>
        <Link href="/studio/artworks/new" className="inline-flex min-h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">添加作品</Link>
      </header>
      <nav aria-label="作品状态筛选" className="flex gap-2 overflow-x-auto border-b border-ink-300 py-4">
        {statuses.map((item) => <Link key={item.label} href={item.value ? `/studio/artworks?status=${item.value}` : "/studio/artworks"} aria-current={item.value === status || (!item.value && !status) ? "page" : undefined} className="min-h-11 shrink-0 rounded-token border border-ink-300 px-4 py-2 text-sm text-ink-700 hover:border-ink-900 hover:text-ink-900">{item.label}</Link>)}
      </nav>
      {artworks.data.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">当前筛选下暂无作品。</p> : <ul className="divide-y divide-ink-300 border-b border-ink-300">{artworks.data.items.map((artwork) => <li key={artwork.id} className="py-5"><Link href={`/studio/artworks/${artwork.id}`} className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-center"><span><span className="block text-lg text-ink-900">{artwork.title}</span><span className="mt-1 block text-sm text-ink-500">{artwork.category} · {artwork.priceAmount} {artwork.priceCurrency}</span></span><span className="text-sm text-ink-700">{artwork.status} · 库存 {artwork.stock}</span></Link></li>)}</ul>}
      {artworks.data.hasMore && artworks.data.nextCursor ? <Link href={`/studio/artworks?status=${status ?? ""}&cursor=${encodeURIComponent(artworks.data.nextCursor)}`} className="mt-6 inline-flex min-h-11 items-center border-b border-ink-900 text-sm text-ink-900">加载更多</Link> : null}
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}