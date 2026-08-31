"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

type ReviewQueue = components["schemas"]["AdminReviewQueuePage"];
type ReviewItem = components["schemas"]["AdminReviewQueueItem"];

/** Admin 审核队列；批准/拒绝使用既有 Catalog 审核接口并在成功后刷新列表。 */
export default function AdminReviewPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const queue = useQuery<ReviewQueue, Error>({
    queryKey: ["admin-review-queue"],
    enabled: !loading && session?.user.role === "ADMIN",
    queryFn: () => authenticatedFetch<ReviewQueue>("/api/v1/admin/artworks/review-queue?limit=20")
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session || session.user.role !== "ADMIN") return <Message text="只有管理员可以访问此页面。" alert />;
  if (queue.isError) return <Message text={queue.error instanceof ApiError ? queue.error.problem.detail ?? "审核队列加载失败" : "审核队列加载失败"} alert />;
  if (queue.isPending) return <Message text="正在加载审核队列..." />;

  return (
    <main>
      <header className="border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Admin / Review</p><h1 className="mt-2 font-serif text-4xl text-ink-900">作品审核</h1><p className="mt-3 text-sm text-ink-500">审核 Creator 提交的作品，批准后由 Creator 在 Studio 完成发布。</p></header>
      {queue.data.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">当前没有待审核作品。</p> : <ul className="divide-y divide-ink-300 border-b border-ink-300">{queue.data.items.map((item) => <ReviewRow key={item.id} item={item} />)}</ul>}
    </main>
  );
}

function ReviewRow({ item }: { item: ReviewItem }) {
  return <li className="py-5"><Link href={`/admin/artworks/review/${item.id}`} className="block hover:bg-ink-50"><div className="flex flex-col gap-2 lg:flex-row lg:items-start lg:justify-between"><div className="min-w-0"><span className="text-lg text-ink-900 hover:text-brand-500">{item.title}</span><p className="mt-1 text-sm text-ink-500">{item.creator.displayName} · {item.category} · {item.productType} · {item.priceAmount} {item.priceCurrency}</p><p className="mt-3 line-clamp-2 text-sm text-ink-700">{item.description}</p></div><span className="shrink-0 text-sm text-brand-600">查看并审核</span></div></Link></li>;
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}
