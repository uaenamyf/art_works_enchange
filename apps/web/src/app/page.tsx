import { Tabs } from "@/components/ui/Tabs";
import { ArtworkCard } from "@/components/domain/ArtworkCard";
import type { ArtworkCardData } from "@/components/domain/types";
import { apiFetch } from "@/lib/api/fetcher";
import Link from "next/link";

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

interface ArtworkListResponse {
  items: ArtworkListItem[];
  hasMore: boolean;
  nextCursor: string | null;
}

interface ArtworkListItem {
  id: string;
  title: string;
  priceAmount: string;
  priceCurrency: string;
  stock: number;
  creatorId: string;
  coverMediaId: string;
  status: "PUBLISHED";
  publishedAt: string;
}

interface PublicMedia {
  id: string;
  url: string;
}

interface CreatorDetail {
  id: string;
  displayName: string;
}

interface PageProps {
  searchParams: Promise<{ query?: string; category?: string; cursor?: string }>;
}

/** Discover 页面：读取公开作品列表，并将媒体与 Creator 快照映射为卡片视图。 */
export default async function DiscoverPage({ searchParams }: PageProps) {
  const filters = await searchParams;
  const query = filters.query?.trim() ?? "";
  const cursor = filters.cursor ?? "";
  const category = categories.includes(filters.category as (typeof categories)[number])
    ? (filters.category ?? "")
    : "";
  const params = new URLSearchParams({ sort: "LATEST", limit: "20" });
  if (query) params.set("query", query);
  if (category) params.set("category", category);
  if (cursor) params.set("cursor", cursor);
  const response = await apiFetch<ArtworkListResponse>(`/api/v1/artworks?${params.toString()}`);
  const latest = await toCardData(response.items);

  return (
    <div className="mx-auto w-full max-w-gallery px-6 py-16">
      <section className="mb-10 max-w-2xl">
        <h1 className="text-3xl text-ink-900 md:text-4xl">发现独立创作者</h1>
        <p className="mt-3 text-base text-ink-700">
          每一件作品背后都是一个人。看看他们最近在做什么，收藏你喜欢的创作者。
        </p>
      </section>

      <form method="get" className="mb-8 grid gap-3 border-y border-ink-300 py-4 md:grid-cols-[1fr_14rem_auto]">
        <label className="flex flex-col gap-1.5 text-sm text-ink-700">
          搜索作品
          <input
            name="query"
            defaultValue={query}
            placeholder="输入标题或描述"
            className="h-11 rounded-token border border-ink-300 bg-white px-3 text-ink-900 focus-visible:border-ink-900 focus-visible:outline-none"
          />
        </label>
        <label className="flex flex-col gap-1.5 text-sm text-ink-700">
          类别
          <select
            name="category"
            defaultValue={category}
            className="h-11 rounded-token border border-ink-300 bg-white px-3 text-ink-900 focus-visible:border-ink-900 focus-visible:outline-none"
          >
            <option value="">全部类别</option>
            {categories.map((item) => <option key={item}>{item}</option>)}
          </select>
        </label>
        <div className="flex items-end gap-3">
          <button type="submit" className="h-11 rounded-token bg-ink-900 px-5 text-sm font-medium text-white hover:bg-ink-700">
            搜索
          </button>
          {query || category ? <Link href="/" className="pb-3 text-sm text-ink-500 hover:text-ink-900">清除</Link> : null}
        </div>
      </form>

      <Tabs
        items={[
          {
            value: "latest",
            label: "最新",
            content: <ArtworkGrid items={latest} priorityFirst nextCursor={response.nextCursor} query={query} category={category} />
          },
          {
            value: "popular",
            label: "热门",
            content: <ArtworkGrid items={latest} nextCursor={response.nextCursor} query={query} category={category} />
          },
          {
            value: "trending",
            label: "趋势",
            content: <ArtworkGrid items={latest} nextCursor={response.nextCursor} query={query} category={category} />
          }
        ]}
      />
    </div>
  );
}

async function toCardData(items: ArtworkListItem[]): Promise<ArtworkCardData[]> {
  return Promise.all(
    items.map(async (item) => {
      const [media, creator] = await Promise.all([
        apiFetch<PublicMedia>(`/api/v1/media/${item.coverMediaId}`),
        apiFetch<CreatorDetail>(`/api/v1/creators/${item.creatorId}`)
      ]);
      return {
        id: item.id,
        title: item.title,
        creatorId: item.creatorId,
        creatorName: creator.displayName,
        priceCents: Math.round(Number(item.priceAmount) * 100),
        currency: item.priceCurrency,
        imageUrl: media.url,
        aspect: "4/5",
        publishedAt: item.publishedAt,
        isFavorite: false,
        isForSale: item.stock > 0
      } satisfies ArtworkCardData;
    })
  );
}

function ArtworkGrid({
  items,
  priorityFirst = false,
  nextCursor,
  query,
  category
}: {
  items: ArtworkCardData[];
  priorityFirst?: boolean;
  nextCursor: string | null;
  query: string;
  category: string;
}) {
  if (items.length === 0) {
    return (
      <div className="border-y border-ink-300 py-16 text-center">
        <h2 className="text-xl text-ink-900">暂时没有已发布作品</h2>
        <p className="mt-2 text-sm text-ink-500">新的作品会在通过审核后出现在这里。</p>
      </div>
    );
  }

  return (
    <>
      <ul className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {items.map((artwork, index) => (
          <li key={artwork.id}>
            <ArtworkCard
              artwork={artwork}
              priority={priorityFirst && index === 0}
            />
          </li>
        ))}
      </ul>
      {nextCursor ? (
        <div className="mt-10 text-center">
          <Link
            href={`/?${new URLSearchParams({ ...(query ? { query } : {}), ...(category ? { category } : {}), cursor: nextCursor }).toString()}`}
            className="inline-flex h-11 items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900 hover:bg-ink-900 hover:text-white"
          >
            加载更多作品
          </Link>
        </div>
      ) : null}
    </>
  );
}
