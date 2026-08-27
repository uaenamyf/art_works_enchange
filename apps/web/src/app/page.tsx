import { Tabs } from "@/components/ui/Tabs";
import { ArtworkCard } from "@/components/domain/ArtworkCard";
import type { ArtworkCardData } from "@/components/domain/types";
import { apiFetch } from "@/lib/api/fetcher";

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

/** Discover 页面：读取公开作品列表，并将媒体与 Creator 快照映射为卡片视图。 */
export default async function DiscoverPage() {
  const response = await apiFetch<ArtworkListResponse>("/api/v1/artworks?sort=LATEST&limit=20");
  const latest = await toCardData(response.items);

  return (
    <div className="mx-auto w-full max-w-gallery px-6 py-16">
      <section className="mb-10 max-w-2xl">
        <h1 className="text-3xl text-ink-900 md:text-4xl">发现独立创作者</h1>
        <p className="mt-3 text-base text-ink-700">
          每一件作品背后都是一个人。看看他们最近在做什么，收藏你喜欢的创作者。
        </p>
      </section>

      <Tabs
        items={[
          {
            value: "latest",
            label: "最新",
            content: <ArtworkGrid items={latest} priorityFirst />
          },
          {
            value: "popular",
            label: "热门",
            content: <ArtworkGrid items={latest} />
          },
          {
            value: "trending",
            label: "趋势",
            content: <ArtworkGrid items={latest} />
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
  priorityFirst = false
}: {
  items: ArtworkCardData[];
  priorityFirst?: boolean;
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
  );
}
