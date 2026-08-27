"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api/fetcher";

interface UserSummary {
  id: string;
  displayName: string;
  avatarUrl: string | null;
  role: string;
}

interface FollowerPageResponse {
  items: UserSummary[];
  nextCursor: string | null;
  hasMore: boolean;
}

/** Creator 公开粉丝列表，使用服务端游标分页并提供完整加载、空态和错误态。 */
export function FollowersList({ creatorId }: { creatorId: string }) {
  const [response, setResponse] = useState<FollowerPageResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);

  useEffect(() => {
    void apiFetch<FollowerPageResponse>(`/api/v1/creators/${creatorId}/followers?limit=12`)
      .then(setResponse)
      .catch((requestError: unknown) => {
        setError(requestError instanceof ApiError ? requestError.problem.detail ?? "粉丝列表加载失败" : "粉丝列表加载失败");
      });
  }, [creatorId]);

  if (error) return <p role="alert" className="mt-4 text-sm text-error">{error}</p>;
  if (!response) return <p className="mt-4 text-sm text-ink-500">正在加载粉丝...</p>;
  if (response.items.length === 0) return <p className="mt-4 text-sm text-ink-500">还没有关注者。</p>;

  async function loadMore() {
    const current = response;
    if (!current?.nextCursor || loadingMore) return;
    setLoadingMore(true);
    setError(null);
    try {
      const next = await apiFetch<FollowerPageResponse>(
        `/api/v1/creators/${creatorId}/followers?cursor=${encodeURIComponent(current.nextCursor)}&limit=12`
      );
      setResponse({ ...next, items: [...current.items, ...next.items] });
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.problem.detail ?? "粉丝列表加载失败" : "粉丝列表加载失败");
    } finally {
      setLoadingMore(false);
    }
  }

  return (
    <>
      <ul className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {response.items.map((user) => (
          <li key={user.id} className="flex min-h-11 items-center gap-3 border-b border-ink-300 py-2">
            <span className="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full bg-ink-100 text-xs text-ink-500">
              {user.avatarUrl ? <Image src={user.avatarUrl} alt="" width={36} height={36} className="size-full object-cover" /> : user.displayName.slice(0, 1)}
            </span>
            <span className="truncate text-sm text-ink-900">{user.displayName}</span>
          </li>
        ))}
      </ul>
      {response.hasMore && response.nextCursor ? (
        <button
          type="button"
          disabled={loadingMore}
          onClick={() => void loadMore()}
          className="mt-5 inline-flex h-11 items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900 hover:bg-ink-900 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loadingMore ? "正在加载..." : "加载更多关注者"}
        </button>
      ) : null}
    </>
  );
}