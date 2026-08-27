import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArtworkCard } from "@/components/domain/ArtworkCard";
import { ROUTES } from "@/lib/config";
import { ApiError, apiFetch } from "@/lib/api/fetcher";
import type { ArtworkCardData } from "@/components/domain/types";

interface PageProps {
  params: Promise<{ id: string }>;
}

interface CreatorDetail {
  id: string;
  displayName: string;
  slug: string;
  category: string;
  bio: string;
  location: string | null;
  avatarUrl: string | null;
  coverUrl: string | null;
  followerCount: number;
  artworkCount: number;
}

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
  publishedAt: string;
}

interface PublicMedia {
  id: string;
  url: string;
}

/** Creator 公开主页：读取真实 Creator 详情与已发布作品，关注动作留给 Week 6。 */
export default async function CreatorProfilePage({ params }: PageProps) {
  const { id } = await params;
  let creator: CreatorDetail;
  try {
    creator = await apiFetch<CreatorDetail>(`/api/v1/creators/${id}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  }
  const artworkResponse = await apiFetch<ArtworkListResponse>(
    `/api/v1/artworks?creatorId=${encodeURIComponent(creator.id)}&sort=LATEST&limit=20`
  );
  const works = await toCardData(artworkResponse.items);
  const coverUrl = creator.coverUrl ?? "";

  return (
    <div>
      {coverUrl ? (
        <div className="relative aspect-[16/9] w-full overflow-hidden bg-ink-100">
          <Image src={coverUrl} alt={`${creator.displayName} 封面`} fill sizes="100vw" className="object-cover" priority />
        </div>
      ) : <div className="aspect-[16/9] w-full bg-ink-100" aria-hidden />}
      <div className="mx-auto w-full max-w-gallery px-6 py-16">
        <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="flex items-end gap-4">
            <span className="relative -mt-12 flex size-24 items-center justify-center overflow-hidden rounded-full border-4 border-white bg-ink-100 shadow-card">
              {creator.avatarUrl ? <Image src={creator.avatarUrl} alt="" fill sizes="96px" className="object-cover" /> : <span className="text-sm text-ink-500">Creator</span>}
            </span>
            <div>
              <h1 className="text-2xl text-ink-900 md:text-3xl">{creator.displayName}</h1>
              <p className="text-sm text-ink-500">
                @{creator.slug}{creator.location ? ` · ${creator.location}` : ""}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="h-11 rounded-token border border-ink-900 px-4 text-sm text-ink-900 hover:bg-ink-900 hover:text-white"
            >
              关注
            </button>
            <Link
              href={ROUTES.home}
              className="text-sm text-ink-500 hover:text-ink-900"
            >
              ← 返回
            </Link>
          </div>
        </header>

        <section className="mt-8 grid gap-8 md:grid-cols-[2fr_1fr]">
          <p className="text-base text-ink-700">{creator.bio}</p>
          <dl className="grid grid-cols-2 gap-4 rounded-token border border-ink-300 p-4 text-sm">
            <div>
                <dt className="text-ink-500">关注者</dt>
                <dd className="mt-1 text-lg text-ink-900">{creator.followerCount.toLocaleString()}</dd>
            </div>
            <div>
              <dt className="text-ink-500">已发布</dt>
                <dd className="mt-1 text-lg text-ink-900">{creator.artworkCount}</dd>
            </div>
          </dl>
        </section>

        <section className="mt-12">
          <h2 className="text-xl text-ink-900">作品</h2>
          {works.length === 0 ? (
            <p className="mt-4 text-ink-500">这位创作者还没有发布作品。</p>
          ) : (
            <ul className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {works.map((artwork) => (
                <li key={artwork.id}>
                  <ArtworkCard artwork={artwork} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}

async function toCardData(items: ArtworkListItem[]): Promise<ArtworkCardData[]> {
  return Promise.all(
    items.map(async (item) => {
      const media = await apiFetch<PublicMedia>(`/api/v1/media/${item.coverMediaId}`);
      return {
        id: item.id,
        title: item.title,
        creatorId: item.creatorId,
        creatorName: "",
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
