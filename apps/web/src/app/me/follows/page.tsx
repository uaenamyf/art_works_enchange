"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { CreatorCard } from "@/components/domain/CreatorCard";
import type { CreatorCardData } from "@/components/domain/types";
import { useAuth } from "@/features/auth/AuthProvider";
import { ApiError } from "@/lib/api/fetcher";
import { ROUTES } from "@/lib/config";

interface FollowedCreator {
  id: string;
  slug: string;
  displayName: string;
  avatarUrl: string | null;
  category: string;
  verified: boolean;
  followerCount: number;
  followedByMe: boolean;
}

interface FollowPageResponse {
  items: FollowedCreator[];
  nextCursor: string | null;
  hasMore: boolean;
}

/** 当前用户的关注列表，访问令牌只通过 AuthProvider 注入请求。 */
export default function FollowsPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const [response, setResponse] = useState<FollowPageResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cursor, setCursor] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    const query = new URLSearchParams(window.location.search);
    const initialCursor = query.get("cursor");
    setCursor(initialCursor);
    const path = initialCursor
      ? `/api/v1/me/follows?cursor=${encodeURIComponent(initialCursor)}`
      : "/api/v1/me/follows";
    void authenticatedFetch<FollowPageResponse>(path)
      .then(setResponse)
      .catch((requestError: unknown) => {
        setError(requestError instanceof ApiError ? requestError.problem.detail ?? "关注列表加载失败" : "关注列表加载失败");
      });
  }, [authenticatedFetch, session]);

  if (loading) return <PageMessage message="正在加载账户..." />;
  if (!session) {
    return (
      <main className="mx-auto w-full max-w-gallery px-6 py-16">
        <h1 className="text-5xl text-ink-900">我的关注</h1>
        <p className="mt-4 text-ink-500">登录后查看关注的创作者。</p>
        <Link href={ROUTES.authLogin} className="mt-6 inline-flex h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">去登录</Link>
      </main>
    );
  }
  if (error) return <PageMessage message={error} alert />;
  if (!response) return <PageMessage message="正在加载关注..." />;

  return (
    <main className="mx-auto w-full max-w-gallery px-6 pb-20 pt-12">
      <header className="mb-10 border-b border-ink-300 pb-6">
        <p className="text-xs uppercase tracking-[0.2em] text-brand-500">CreatorHub / 关注</p>
        <h1 className="mt-3 text-5xl text-ink-900">我的关注</h1>
      </header>
      {response.items.length === 0 ? (
        <p className="border-b border-ink-300 py-12 text-sm text-ink-500">还没有关注创作者。</p>
      ) : (
        <ul className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {response.items.map((creator) => <li key={creator.id}><CreatorCard creator={toCardData(creator)} /></li>)}
        </ul>
      )}
      {response.hasMore && response.nextCursor ? (
        <Link
          href={`${ROUTES.follows}?cursor=${encodeURIComponent(response.nextCursor)}`}
          className="mx-auto mt-10 flex h-11 w-fit items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900 hover:bg-ink-900 hover:text-white"
        >
          加载更多创作者
        </Link>
      ) : null}
      {cursor ? <Link href={ROUTES.follows} className="mx-auto mt-4 flex w-fit text-sm text-ink-500 hover:text-ink-900">返回第一页</Link> : null}
    </main>
  );
}

function toCardData(creator: FollowedCreator): CreatorCardData {
  return {
    id: creator.id,
    name: creator.displayName,
    handle: `@${creator.slug}`,
    avatarUrl: creator.avatarUrl,
    coverUrl: null,
    bio: "",
    location: "",
    followers: creator.followerCount,
    publishedArtworks: 0
  };
}

function PageMessage({ message, alert = false }: { message: string; alert?: boolean }) {
  return <main className="mx-auto w-full max-w-gallery px-6 py-16"><h1 className="text-5xl text-ink-900">我的关注</h1><p role={alert ? "alert" : undefined} className={`mt-6 text-sm ${alert ? "text-error" : "text-ink-500"}`}>{message}</p></main>;
}