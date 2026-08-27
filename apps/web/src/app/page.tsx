import { ArtworkCard } from "@/components/domain/ArtworkCard";
import type { ArtworkCardData } from "@/components/domain/types";
import { apiFetch } from "@/lib/api/fetcher";
import Link from "next/link";

const categories = [
  ["艺术品", "ART"],
  ["摄影", "PHOTOGRAPHY"],
  ["陶艺", "CERAMICS"],
  ["木制品", "WOODWORK"],
  ["皮具", "LEATHER"],
  ["珠宝", "JEWELRY"],
  ["时装", "FASHION"],
  ["数字作品", "DIGITAL"],
  ["收藏品", "COLLECTIBLES"]
] as const;

interface ArtworkListResponse {
  items: ArtworkListItem[];
  hasMore: boolean;
  nextCursor: string | null;
}

interface ArtworkListItem {
  id: string;
  title: string;
  category: string;
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

interface PageProps { searchParams: Promise<{ query?: string; category?: string; cursor?: string }> }

/** Discover 页面：读取公开作品列表，并将媒体与 Creator 快照映射为卡片视图。 */
export default async function DiscoverPage({ searchParams }: PageProps) {
  const filters = await searchParams;
  const query = filters.query?.trim() ?? "";
  const cursor = filters.cursor ?? "";
  const category = categories.some(([, value]) => value === filters.category) ? (filters.category ?? "") : "";
  const params = new URLSearchParams({ sort: "LATEST", limit: "20" });
  if (query) params.set("query", query);
  if (category) params.set("category", category);
  if (cursor) params.set("cursor", cursor);
  const response = await apiFetch<ArtworkListResponse>(`/api/v1/artworks?${params.toString()}`);
  const latest = await toCardData(response.items);

  return (
    <div className="mx-auto w-full max-w-[1280px] px-5 pb-20 pt-16 lg:px-8 lg:pt-24">
      <section className="mb-16 max-w-3xl">
        <p className="text-xs uppercase tracking-[0.22em] text-brand-500">CreatorHub / 独立创作</p>
        <h1 className="mt-4 text-5xl leading-[1.05] text-ink-900 md:text-7xl">发现正在发生的创作</h1>
        <p className="mt-6 max-w-xl text-base leading-7 text-ink-700">从一件作品开始，遇见它背后的人。</p>
      </section>

      <section aria-label="按类别浏览作品" className="space-y-16">
        {latest.length === 0 ? (
          <div className="border-y border-ink-300 py-20 text-center">
            <p className="text-xs uppercase tracking-[0.18em] text-ink-500">作品目录</p>
            <h2 className="mt-3 text-3xl text-ink-900">新的创作即将到来</h2>
            <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-ink-500">
              作品通过审核后，会按照艺术品、木制品、摄影等类别出现在这里。
            </p>
            <Link href="/creators" className="mt-6 inline-flex text-sm text-ink-900 underline decoration-ink-300 underline-offset-4 hover:decoration-brand-500">
              先去认识创作者
            </Link>
          </div>
        ) : categories
            .filter(([, value]) => !category || category === value)
            .map(([label, value]) => (
              <CategorySection
                key={value}
                label={label}
                value={value}
                items={latest.filter((artwork) => artwork.category === value)}
                query={query}
                cursor={category === value ? response.nextCursor : null}
              />
            ))}
      </section>
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
        category: item.category,
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

function CategorySection({
  items,
  label,
  value,
  query,
  cursor
}: {
  items: ArtworkCardData[];
  label: string;
  value: string;
  query: string;
  cursor: string | null;
}) {
  return (
    <section>
      <div className="mb-5 flex items-end justify-between border-b border-ink-300 pb-4">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-ink-500">作品类别</p>
          <h2 className="mt-1 text-3xl text-ink-900">{label}</h2>
        </div>
        <Link href={`/?category=${value}`} className="text-sm text-ink-500 hover:text-ink-900">查看全部</Link>
      </div>
      {items.length === 0 ? (
        <div className="border-b border-ink-300 py-12 text-sm text-ink-500">暂无已发布作品</div>
      ) : (
        <>
          <ul className="flex snap-x gap-4 overflow-x-auto pb-3">
            {items.map((artwork, index) => (
              <li key={artwork.id} className="w-[72vw] shrink-0 snap-start sm:w-[40vw] md:w-[30vw] lg:w-[23vw]">
                <ArtworkCard artwork={artwork} priority={index === 0} />
              </li>
            ))}
          </ul>
          {cursor ? (
            <div className="mt-10 text-center">
              <Link
                href={`/?${new URLSearchParams({ ...(query ? { query } : {}), category: value, cursor }).toString()}`}
                className="inline-flex h-11 items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900 hover:bg-ink-900 hover:text-white"
              >
                加载更多作品
              </Link>
            </div>
          ) : null}
        </>
      )}
    </section>
  );
}
