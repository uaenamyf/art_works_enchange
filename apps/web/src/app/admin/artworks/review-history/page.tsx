"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

type ReviewHistory = components["schemas"]["AdminReviewHistoryPage"];
type ReviewHistoryItem = components["schemas"]["AdminReviewHistoryItem"];

/** Admin 审核历史；只读取审核事实，使用服务端不透明游标加载后续记录。 */
export default function AdminReviewHistoryPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const [cursor, setCursor] = useState<string | undefined>();
  const history = useQuery<ReviewHistory, Error>({
    queryKey: ["admin-review-history", cursor],
    enabled: !loading && session?.user.role === "ADMIN",
    queryFn: () => {
      const suffix = cursor ? `?cursor=${encodeURIComponent(cursor)}&limit=20` : "?limit=20";
      return authenticatedFetch<ReviewHistory>(`/api/v1/admin/artworks/review-history${suffix}`);
    }
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session || session.user.role !== "ADMIN") return <Message text="只有管理员可以访问此页面。" alert />;
  if (history.isError) {
    return (
      <Message
        text={history.error instanceof ApiError ? history.error.problem.detail ?? "审核历史加载失败" : "审核历史加载失败"}
        alert
      />
    );
  }
  if (history.isPending) return <Message text="正在加载审核历史..." />;

  return (
    <main>
      <header className="border-b border-ink-300 pb-6">
        <p className="text-xs uppercase tracking-[0.18em] text-brand-600">Admin / Review History</p>
        <h1 className="mt-2 font-serif text-4xl text-ink-900">审核历史</h1>
        <p className="mt-3 text-sm text-ink-500">查看已完成的作品审核决定、理由和审核人，记录按时间倒序排列。</p>
      </header>
      {history.data.items.length === 0 ? (
        <p className="border-b border-ink-300 py-12 text-sm text-ink-500">当前没有审核记录。</p>
      ) : (
        <>
          <ul className="divide-y divide-ink-300 border-b border-ink-300">
            {history.data.items.map((item) => <HistoryRow key={item.id} item={item} />)}
          </ul>
          {history.data.hasMore && history.data.nextCursor ? (
            <button
              type="button"
              className="mt-6 min-h-11 border border-ink-400 px-4 text-sm text-ink-800 hover:border-brand-500 hover:text-brand-600"
              onClick={() => setCursor(history.data.nextCursor ?? undefined)}
            >
              加载下一页
            </button>
          ) : null}
        </>
      )}
    </main>
  );
}

function HistoryRow({ item }: { item: ReviewHistoryItem }) {
  const decisionLabel = item.decision === "APPROVED" ? "已批准" : "已拒绝";
  return (
    <li className="py-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <p className="text-lg text-ink-900">{item.artworkTitle}</p>
          <p className="mt-1 text-sm text-ink-500">
            {item.creator.displayName} · {decisionLabel} · {formatDate(item.createdAt)}
          </p>
          <p className="mt-3 text-sm text-ink-700">{item.reason}</p>
        </div>
        <span className="shrink-0 text-sm text-ink-500">审核人 {item.reviewerId.slice(0, 8)}</span>
      </div>
    </li>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}
