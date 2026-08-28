"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";

interface NotificationItem {
  id: string;
  type: string;
  title: string;
  body: string;
  link?: string | null;
  readAt: string | null;
  createdAt: string;
}

interface NotificationPageResponse {
  items: NotificationItem[];
  nextCursor: string | null;
  hasMore: boolean;
  unreadCount: number;
}

/** 站内通知页；只读取当前用户通知，并通过服务端接口标记已读。 */
export default function NotificationsPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [loadingItems, setLoadingItems] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    setLoadingItems(true);
    setError(null);
    void authenticatedFetch<NotificationPageResponse>(
      `/api/v1/notifications?limit=20&unreadOnly=${unreadOnly}`
    )
      .then((page) => {
        setItems(page.items);
        setNextCursor(page.nextCursor);
        setHasMore(page.hasMore);
        setUnreadCount(page.unreadCount);
      })
      .catch((requestError: unknown) =>
        setError(
          requestError instanceof ApiError
            ? requestError.problem.detail ?? "通知加载失败"
            : "通知加载失败"
        )
      )
      .finally(() => setLoadingItems(false));
  }, [authenticatedFetch, session, unreadOnly]);

  async function loadMore() {
    if (!nextCursor || loadingItems) return;
    setLoadingItems(true);
    try {
      const page = await authenticatedFetch<NotificationPageResponse>(
        `/api/v1/notifications?limit=20&unreadOnly=${unreadOnly}&cursor=${encodeURIComponent(nextCursor)}`
      );
      setItems((current) => [...current, ...page.items]);
      setNextCursor(page.nextCursor);
      setHasMore(page.hasMore);
      setUnreadCount(page.unreadCount);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.problem.detail ?? "更多通知加载失败"
          : "更多通知加载失败"
      );
    } finally {
      setLoadingItems(false);
    }
  }

  async function markRead(notificationId: string) {
    try {
      await authenticatedFetch(`/api/v1/notifications/${notificationId}/read`, { method: "POST" });
      setItems((current) =>
        current.map((item) =>
          item.id === notificationId && !item.readAt
            ? { ...item, readAt: new Date().toISOString() }
            : item
        )
      );
      setUnreadCount((count) => Math.max(0, count - 1));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.problem.detail ?? "标记已读失败"
          : "标记已读失败"
      );
    }
  }

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录后查看通知。" />;
  if (error && items.length === 0) return <Message text={error} alert />;

  return (
    <main className="mx-auto w-full max-w-[960px] px-6 py-12">
      <header className="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-ink-300 pb-6">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-brand-500">CreatorHub / 通知</p>
          <h1 className="mt-3 text-4xl text-ink-900">我的通知</h1>
          <p className="mt-2 text-sm text-ink-500">{unreadCount} 条未读通知</p>
        </div>
        <label className="flex min-h-11 items-center gap-2 text-sm text-ink-700">
          <input type="checkbox" checked={unreadOnly} onChange={(event) => setUnreadOnly(event.target.checked)} />
          只看未读
        </label>
      </header>
      {loadingItems && items.length === 0 ? <p role="status" className="py-12 text-sm text-ink-500">正在加载通知...</p> : null}
      {!loadingItems && items.length === 0 ? <p className="border-y border-ink-300 py-12 text-sm text-ink-500">暂无通知。</p> : null}
      {items.length > 0 ? <ul className="divide-y divide-ink-300 border-y border-ink-300">{items.map((item) => <li key={item.id} className={item.readAt ? "py-5" : "border-l-2 border-brand-500 py-5 pl-4"}><div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between"><div><p className="text-lg text-ink-900">{item.title}</p><p className="mt-1 text-sm leading-6 text-ink-700">{item.body}</p><p className="mt-2 text-xs text-ink-500">{new Date(item.createdAt).toLocaleString("zh-CN")}</p></div><div className="flex shrink-0 items-center gap-3">{item.link ? <Link href={item.link} className="text-sm text-brand-600 hover:text-brand-800">查看</Link> : null}{!item.readAt ? <button type="button" className="min-h-11 text-sm text-ink-700 underline underline-offset-4 hover:text-ink-900" onClick={() => void markRead(item.id)}>标记已读</button> : null}</div></div></li>)}</ul> : null}
      {error && items.length > 0 ? <p role="alert" className="mt-4 text-sm text-error">{error}</p> : null}
      {hasMore ? <button type="button" className="mt-6 min-h-11 border-b border-ink-900 text-sm text-ink-900" disabled={loadingItems} onClick={() => void loadMore()}>{loadingItems ? "正在加载..." : "加载更多"}</button> : null}
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="mx-auto w-full max-w-[960px] px-6 py-16"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}