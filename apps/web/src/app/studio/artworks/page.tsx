"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import { hasCreatorWorkspaceAccess } from "@/features/auth/types";
import type { components } from "@/lib/api/schema.gen";
import { useLocale } from "@/features/locale/LocaleProvider";

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
  const { locale, t } = useLocale();
  const searchParams = typeof window === "undefined" ? new URLSearchParams() : new URLSearchParams(window.location.search);
  const query = searchParams.get("status") ?? "";
  const cursor = searchParams.get("cursor");
  const status = statuses.find((item) => item.value === query)?.value;
  const isCreator = hasCreatorWorkspaceAccess(session);
  const artworks = useQuery<ArtworkPage, Error>({
    queryKey: ["studio-artworks", { status }],
    enabled: isCreator && !loading,
    queryFn: () => {
      const params = new URLSearchParams({ limit: "20" });
      if (status) params.set("status", status);
      if (cursor) params.set("cursor", cursor);
      return authenticatedFetch<ArtworkPage>(`/api/v1/studio/artworks?${params.toString()}`);
    }
  });

  if (loading) return <Message text={t("loadingAccount")} />;
  if (!session) return <Message text={t("loginCreatorRequired")} alert />;
  if (!isCreator) return <Message text={t("creatorOnboardingRequired")} href="/studio/creator/apply" />;
  if (artworks.isError) return <Message text={locale === "en-US" ? t("artworksLoadFailed") : artworks.error instanceof ApiError ? artworks.error.problem.detail ?? t("artworksLoadFailed") : t("artworksLoadFailed")} alert />;
  if (artworks.isPending) return <Message text={t("loadingArtworks")} />;

  return (
    <main>
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-ink-300 pb-6">
        <div><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Works</p><h1 className="mt-2 font-serif text-4xl text-ink-900">{t("artworkManagement")}</h1><p className="mt-3 text-sm text-ink-500">{t("artworkManagementHint")}</p></div>
        <Link href="/studio/artworks/new" className="inline-flex min-h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">{t("addArtwork")}</Link>
      </header>
      <ol aria-label={t("artworkFlow")} className="grid gap-3 border-b border-ink-300 py-5 sm:grid-cols-4">
        {[
          ["01", t("createDraft")],
          ["02", t("uploadAssets")],
          ["03", t("submitArtwork")],
          ["04", t("publishAfterReview")]
        ].map(([number, label]) => (
          <li key={number} className="flex items-center gap-3 text-sm text-ink-700">
            <span className="text-xs text-brand-600">{number}</span>
            <span>{label}</span>
          </li>
        ))}
      </ol>
      <nav aria-label={t("artworkStatusFilter")} className="flex gap-2 overflow-x-auto border-b border-ink-300 py-4">
        {statuses.map((item) => <Link key={item.value ?? "all"} href={item.value ? `/studio/artworks?status=${item.value}` : "/studio/artworks"} aria-current={item.value === status || (!item.value && !status) ? "page" : undefined} className="min-h-11 shrink-0 rounded-token border border-ink-300 px-4 py-2 text-sm text-ink-700 hover:border-ink-900 hover:text-ink-900">{item.value ? t(item.value === "DRAFT" ? "draft" : item.value === "PENDING_REVIEW" ? "pendingReview" : item.value === "PUBLISHED" ? "published" : "archived") : t("all")}</Link>)}
      </nav>
      {artworks.data.items.length === 0 ? <p className="border-b border-ink-300 py-12 text-sm text-ink-500">{t("noArtworksInFilter")}</p> : <ul className="divide-y divide-ink-300 border-b border-ink-300">{artworks.data.items.map((artwork) => <li key={artwork.id} className="py-5"><Link href={`/studio/artworks/${artwork.id}`} className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-center"><span><span className="block text-lg text-ink-900">{artwork.title}</span><span className="mt-1 block text-sm text-ink-500">{artwork.category} · {artwork.priceAmount} {artwork.priceCurrency}</span></span><span className="text-sm text-ink-700">{artwork.status} · {artwork.stock}</span></Link></li>)}</ul>}
      {artworks.data.hasMore && artworks.data.nextCursor ? <Link href={`/studio/artworks?status=${status ?? ""}&cursor=${encodeURIComponent(artworks.data.nextCursor)}`} className="mt-6 inline-flex min-h-11 items-center border-b border-ink-900 text-sm text-ink-900">{t("loadMore")}</Link> : null}
    </main>
  );
}

function Message({ text, alert = false, href }: { text: string; alert?: boolean; href?: string }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p>{href ? <Link href={href} className="mt-5 inline-flex min-h-11 items-center rounded-token bg-ink-900 px-4 text-sm text-white">申请成为 Creator</Link> : null}</main>;
}