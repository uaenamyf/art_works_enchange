"use client";

import Image from "next/image";
import Link from "next/link";
import { useQueries, useQuery } from "@tanstack/react-query";
import { FavoriteButton } from "@/features/favorites/FavoriteButton";
import { useAuth } from "@/features/auth/AuthProvider";
import { ApiError } from "@/lib/api/fetcher";
import { ROUTES } from "@/lib/config";
import type { components } from "@/lib/api/schema.gen";

const FAVORITES_API_PATH = "/api/v1/me/favorites";

type FavoritePageResponse = components["schemas"]["FavoritePage"];
type MediaResponse = components["schemas"]["Media"];

/** 当前用户的收藏工作台，访问令牌只通过 AuthProvider 注入请求。 */
export default function FavoritesPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const favorites = useQuery<FavoritePageResponse, Error>({
    queryKey: ["favorites", "mine"],
    enabled: Boolean(session) && !loading,
    queryFn: () => authenticatedFetch<FavoritePageResponse>(FAVORITES_API_PATH)
  });
  const mediaQueries = useQueries({
    queries: (favorites.data?.items ?? []).map((item) => {
      const coverId = item.mediaIds[0];
      return {
        queryKey: ["media", coverId],
        enabled: Boolean(session) && Boolean(coverId),
        queryFn: () => authenticatedFetch<MediaResponse>(`/api/v1/media/${coverId}`)
      };
    })
  });
  const media = Object.fromEntries(
    (favorites.data?.items ?? []).flatMap((item, index) => {
      const url = mediaQueries[index]?.data?.url;
      return url ? [[item.id, url] as const] : [];
    })
  );

  if (loading) return <main className="mx-auto w-full max-w-[1280px] px-5 py-16 lg:px-8"><p className="text-sm text-ink-500">正在加载账户...</p></main>;
  if (!session) return <main className="mx-auto w-full max-w-[1280px] px-5 py-16 lg:px-8"><h1 className="text-5xl text-ink-900">我的收藏</h1><p className="mt-4 text-ink-500">登录后查看收藏的作品。</p><Link href={ROUTES.authLogin} className="mt-6 inline-flex h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">去登录</Link></main>;
  if (favorites.isError) return <main className="mx-auto w-full max-w-[1280px] px-5 py-16 lg:px-8"><h1 className="text-5xl text-ink-900">我的收藏</h1><p role="alert" className="mt-6 text-sm text-error">{favorites.error instanceof ApiError ? favorites.error.problem.detail ?? "收藏加载失败" : "收藏加载失败"}</p></main>;
  if (favorites.isPending) return <main className="mx-auto w-full max-w-[1280px] px-5 py-16 lg:px-8"><p className="text-sm text-ink-500">正在加载收藏...</p></main>;
  const response = favorites.data;
  return (
    <main className="mx-auto w-full max-w-[1280px] px-5 pb-20 pt-12 lg:px-8">
      <header className="mb-10 border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.2em] text-brand-500">CreatorHub / 收藏</p><h1 className="mt-3 text-5xl text-ink-900">我的收藏</h1></header>
      {response.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">还没有收藏作品。</p> : (
        <ul className="grid grid-cols-2 gap-x-5 gap-y-10 md:grid-cols-3 lg:grid-cols-4">
          {response.items.map((item) => <li key={item.id} className="min-w-0"><article className="flex flex-col gap-3"><Link href={`/artworks/${item.id}`} className="relative block aspect-[4/5] overflow-hidden rounded-token bg-ink-100" aria-label={`查看作品：${item.title}`}>{media[item.id] ? <Image src={media[item.id]} alt={item.title} fill sizes="(min-width: 1024px) 25vw, (min-width: 640px) 33vw, 50vw" className="object-cover" /> : null}</Link><h2 className="line-clamp-1 text-base text-ink-900">{item.title}</h2><p className="text-sm text-ink-500">{item.category}</p><FavoriteButton artworkId={item.id} initialFavorited /></article></li>)}
        </ul>
      )}
      {response.hasMore && response.nextCursor ? <Link href={`${ROUTES.favorites}?cursor=${encodeURIComponent(response.nextCursor)}`} className="mx-auto mt-10 flex h-11 w-fit items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900">加载更多</Link> : null}
    </main>
  );
}