import { CreatorCard } from "@/components/domain/CreatorCard";
import type { CreatorCardData } from "@/components/domain/types";
import { apiFetch } from "@/lib/api/fetcher";
import Link from "next/link";
import { getServerLocale } from "@/features/locale/serverLocale";

const categories = [
  "ART",
  "PHOTOGRAPHY",
  "CERAMICS",
  "WOODWORK",
  "LEATHER",
  "JEWELRY",
  "FASHION",
  "DIGITAL",
  "COLLECTIBLES"
] as const;

interface PageProps {
  searchParams: Promise<{ category?: string; cursor?: string }>;
}

interface CreatorPageResponse {
  items: CreatorListItem[];
  hasMore: boolean;
  nextCursor: string | null;
}

interface CreatorListItem {
  id: string;
  slug: string;
  displayName: string;
  avatarUrl: string | null;
  category: string;
  verified: boolean;
  followerCount: number;
  followedByMe: boolean;
}

/** Creator 浏览页：支持 URL 分类筛选，列表和分页由公开 Creator API 提供。 */
export default async function CreatorsPage({ searchParams }: PageProps) {
  const locale = await getServerLocale();
  const english = locale === "en-US";
  const filters = await searchParams;
  const category = categories.includes(filters.category as (typeof categories)[number])
    ? filters.category
    : "";
  const cursor = filters.cursor ?? "";
  const params = new URLSearchParams({ limit: "20" });
  if (category) params.set("category", category);
  if (cursor) params.set("cursor", cursor);
  const response = await apiFetch<CreatorPageResponse>(`/api/v1/creators?${params.toString()}`);
  const creators = response.items.map(toCardData);

  return (
    <main className="mx-auto w-full max-w-gallery px-6 py-16">
      <header className="mb-10 max-w-2xl">
        <p className="text-sm uppercase tracking-[0.18em] text-brand-500">Creators</p>
        <h1 className="mt-2 text-4xl text-ink-900">{english ? "Meet the creators" : "认识创作者"}</h1>
        <p className="mt-3 text-base text-ink-700">{english ? "Discover the people who keep creating." : "从作品出发，了解正在持续创作的人。"}</p>
      </header>

      <nav aria-label="创作者类别" className="mb-8 flex flex-wrap gap-2">
        <CategoryLink label={english ? "All" : "全部"} value="" active={!category} />
        {categories.map((item) => <CategoryLink key={item} label={item} value={item} active={category === item} />)}
      </nav>

      {creators.length === 0 ? (
        <div className="border-y border-ink-300 py-16 text-center">
          <h2 className="text-xl text-ink-900">{english ? "No creators found" : "暂时没有找到创作者"}</h2>
          <p className="mt-2 text-sm text-ink-500">{english ? "Try another category." : "换一个类别再看看。"}</p>
        </div>
      ) : (
        <ul className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {creators.map((creator) => <li key={creator.id}><CreatorCard creator={creator} /></li>)}
        </ul>
      )}
      {response.nextCursor ? (
        <div className="mt-10 text-center">
          <Link
            href={`/creators?${new URLSearchParams({ ...(category ? { category } : {}), cursor: response.nextCursor }).toString()}`}
            className="inline-flex h-11 items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900 hover:bg-ink-900 hover:text-white"
          >
            {english ? "Load more creators" : "加载更多创作者"}
          </Link>
        </div>
      ) : null}
    </main>
  );
}

function CategoryLink({ label, value, active }: { label: string; value: string; active: boolean }) {
  const href = value ? `/creators?category=${encodeURIComponent(value)}` : "/creators";
  return (
    <a
      href={href}
      aria-current={active ? "page" : undefined}
      className={`rounded-token border px-3 py-2 text-sm ${active ? "border-ink-900 bg-ink-900 text-white" : "border-ink-300 text-ink-700 hover:border-ink-900"}`}
    >
      {label}
    </a>
  );
}

function toCardData(creator: CreatorListItem): CreatorCardData {
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