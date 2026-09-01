import { ArtworkCard } from "@/components/domain/ArtworkCard";
import type { ArtworkCardData } from "@/components/domain/types";
import { apiFetch } from "@/lib/api/fetcher";

export const artworkCategories = [
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

export type ArtworkCategory = (typeof artworkCategories)[number][1];
export type ProductType = "PHYSICAL" | "DIGITAL";

export const productSections = [
  { label: "实体产品", value: "PHYSICAL" as const, categories: artworkCategories.filter(([, value]) => value !== "DIGITAL") },
  { label: "数字产品", value: "DIGITAL" as const, categories: artworkCategories.filter(([, value]) => value === "DIGITAL") }
] as const;

export function getArtworkCategoryLabel(value: string, english: boolean) {
  const labels: Record<string, string> = {
    ART: "Artwork",
    PHOTOGRAPHY: "Photography",
    CERAMICS: "Ceramics",
    WOODWORK: "Woodwork",
    LEATHER: "Leather",
    JEWELRY: "Jewelry",
    FASHION: "Fashion",
    DIGITAL: "Digital works",
    COLLECTIBLES: "Collectibles"
  };
  return english ? labels[value] ?? value : artworkCategories.find(([, category]) => category === value)?.[0] ?? value;
}

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
  favoriteCount: number;
  favoritedByMe: boolean;
}

interface PublicMedia { id: string; url: string }
interface CreatorDetail { id: string; displayName: string }

export function getArtworkCategory(value: string | undefined) {
  return artworkCategories.find(([, category]) => category === value);
}

export async function loadArtworkCards(options: {
  category?: ArtworkCategory;
  productType?: ProductType;
  query?: string;
  cursor?: string;
  limit?: number;
}) {
  const params = new URLSearchParams({ sort: "LATEST", limit: String(options.limit ?? 20) });
  if (options.category) params.set("category", options.category);
  if (options.productType) params.set("productType", options.productType);
  if (options.query) params.set("query", options.query);
  if (options.cursor) params.set("cursor", options.cursor);
  const response = await apiFetch<ArtworkListResponse>(`/api/v1/artworks?${params.toString()}`);
  return { ...response, items: await toCardData(response.items) };
}

export function ArtworkRow({ items, nextCursor, query, category, layout = "row" }: {
  items: ArtworkCardData[];
  nextCursor: string | null;
  query?: string;
  category: ArtworkCategory;
  layout?: "row" | "grid";
}) {
  if (items.length === 0) return <div className="border-b border-ink-300 py-12 text-sm text-ink-500">暂无已发布作品</div>;
  return (
    <>
      <ul className={layout === "grid" ? "grid min-w-0 grid-cols-2 gap-x-5 gap-y-10 md:grid-cols-3 lg:grid-cols-4" : "flex min-w-0 snap-x gap-4 overflow-x-auto pb-3"}>
        {items.map((artwork, index) => (
          <li key={artwork.id} className={layout === "grid" ? "min-w-0" : "w-[72vw] shrink-0 snap-start sm:w-[40vw] md:w-[30vw] lg:w-[23vw]"}>
            <ArtworkCard artwork={artwork} priority={index === 0} />
          </li>
        ))}
      </ul>
      {nextCursor ? (
        <div className="mt-8 text-center">
          <a href={`/categories/${category}?${new URLSearchParams({ ...(query ? { query } : {}), cursor: nextCursor }).toString()}`} className="inline-flex h-11 items-center rounded-token border border-ink-900 px-5 text-sm text-ink-900 hover:bg-ink-900 hover:text-white">加载更多作品</a>
        </div>
      ) : null}
    </>
  );
}

async function toCardData(items: ArtworkListItem[]): Promise<ArtworkCardData[]> {
  return Promise.all(items.map(async (item) => {
    const [media, creator] = await Promise.all([
      apiFetch<PublicMedia>(`/api/v1/media/${item.coverMediaId}`),
      apiFetch<CreatorDetail>(`/api/v1/creators/${item.creatorId}`)
    ]);
    return {
      id: item.id, title: item.title, category: item.category, creatorId: item.creatorId,
      creatorName: creator.displayName, priceCents: Math.round(Number(item.priceAmount) * 100),
      currency: item.priceCurrency, imageUrl: media.url, aspect: "4/5",
      publishedAt: item.publishedAt, isFavorite: item.favoritedByMe,
      favoriteCount: item.favoriteCount, isForSale: item.stock > 0
    } satisfies ArtworkCardData;
  }));
}