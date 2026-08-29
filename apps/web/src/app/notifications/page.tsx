"use client";

import Link from "next/link";
import { useInfiniteQuery, useMutation, useQueryClient, type InfiniteData } from "@tanstack/react-query";
import { useState } from "react";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

type NotificationPageResponse = components["schemas"]["NotificationPage"];

/** 站内通知页；只读取当前用户通知，并通过服务端接口标记已读。 */
export default function NotificationsPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const queryClient = useQueryClient();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const queryKey = ["notifications", { unreadOnly }] as const;

  const notifications = useInfiniteQuery<
    NotificationPageResponse,
    Error,
    InfiniteData<NotificationPageResponse>,
    typeof queryKey,
    string | null
  >({
    queryKey,
    enabled: Boolean(session) && !loading,
    initialPageParam: null as string | null,
    queryFn: ({ pageParam }: { pageParam: string | null }) => {
      const cursor = pageParam ? `&cursor=${encodeURIComponent(pageParam)}` : "";
      return authenticatedFetch<NotificationPageResponse>(
        `/api/v1/notifications?limit=20&unreadOnly=${unreadOnly}${cursor}`
      );
    },
    placeholderData: (previous) => previous,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore && lastPage.nextCursor ? lastPage.nextCursor : undefined
  });

  const items = notifications.data?.pages.flatMap((page) => page.items) ?? [];
  const unreadCount = notifications.data?.pages[0]?.unreadCount ?? 0;
  const markRead = useMutation<void, unknown, string, { previous?: InfiniteData<NotificationPageResponse> }>({
    mutationFn: (notificationId) =>
      authenticatedFetch<void>(`/api/v1/notifications/${notificationId}/read`, { method: "POST" }),
    onMutate: async (notificationId) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<InfiniteData<NotificationPageResponse>>(queryKey);
      queryClient.setQueryData<InfiniteData<NotificationPageResponse>>(queryKey, (current) => {
        if (!current) return current;
        return {
          ...current,
          pages: current.pages.map((page) => ({
            ...page,
            unreadCount: Math.max(0, page.unreadCount - (page.items.some((item) => item.id === notificationId && !item.readAt) ? 1 : 0)),
            items: page.items.map((item) =>
              item.id === notificationId && !item.readAt
                ? { ...item, readAt: new Date().toISOString() }
                : item
            )
          }))
        };
      });
      return { previous };
    },
    onError: (_error, _notificationId, context) => {
      if (context?.previous) queryClient.setQueryData(queryKey, context.previous);
    }
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session) return <Message text="请先登录后查看通知。" />;
  if (notifications.isError && items.length === 0) return <Message text={getErrorMessage(notifications.error, "通知加载失败")} alert />;

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
      {notifications.isPending ? <p role="status" className="py-12 text-sm text-ink-500">正在加载通知...</p> : null}
      {!notifications.isPending && items.length === 0 ? <p className="border-y border-ink-300 py-12 text-sm text-ink-500">暂无通知。</p> : null}
      {items.length > 0 ? <ul className="divide-y divide-ink-300 border-y border-ink-300">{items.map((item) => <li key={item.id} className={item.readAt ? "py-5" : "border-l-2 border-brand-500 py-5 pl-4"}><div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between"><div><p className="text-lg text-ink-900">{item.title}</p><p className="mt-1 text-sm leading-6 text-ink-700">{item.body}</p><p className="mt-2 text-xs text-ink-500">{new Date(item.createdAt).toLocaleString("zh-CN")}</p></div><div className="flex shrink-0 items-center gap-3">{item.link ? <Link href={item.link} className="text-sm text-brand-600 hover:text-brand-800">查看</Link> : null}{!item.readAt ? <button type="button" className="min-h-11 text-sm text-ink-700 underline underline-offset-4 hover:text-ink-900" onClick={() => markRead.mutate(item.id)}>标记已读</button> : null}</div></div></li>)}</ul> : null}
      {notifications.isError && items.length > 0 ? <p role="alert" className="mt-4 text-sm text-error">{getErrorMessage(notifications.error, "更多通知加载失败")}</p> : null}
      {notifications.hasNextPage ? <button type="button" className="mt-6 min-h-11 border-b border-ink-900 text-sm text-ink-900" disabled={notifications.isFetchingNextPage} onClick={() => void notifications.fetchNextPage()}>{notifications.isFetchingNextPage ? "正在加载..." : "加载更多"}</button> : null}
      {markRead.isError ? <p role="alert" className="mt-4 text-sm text-error">{getErrorMessage(markRead.error, "标记已读失败")}</p> : null}
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="mx-auto w-full max-w-[960px] px-6 py-16"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.problem.detail ?? fallback : fallback;
}