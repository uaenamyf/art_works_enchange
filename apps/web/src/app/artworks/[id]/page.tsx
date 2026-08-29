import { notFound } from "next/navigation";
import Link from "next/link";
import { Share2 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { FavoriteButton } from "@/features/favorites/FavoriteButton";
import { MediaGallery, type MediaGalleryItem } from "@/components/domain/MediaGallery";
import { ROUTES } from "@/lib/config";
import { apiFetch, ApiError } from "@/lib/api/fetcher";

interface PageProps {
  params: Promise<{ id: string }>;
}

type ArtworkDetail = {
  id: string;
  creatorId: string;
  title: string;
  description: string;
  priceAmount: string;
  priceCurrency: string;
  stock: number;
  mediaIds: string[];
  status: string;
  favoriteCount: number;
  favoritedByMe: boolean;
};

type PublicMedia = { id: string; url: string; contentType: string };

/** 公开详情服务端页面：只读取已发布作品，并将媒体 ID 换成短期安全 URL。 */
export default async function ArtworkDetailPage({ params }: PageProps) {
  const { id } = await params;
  let artwork: ArtworkDetail;
  try {
    artwork = await apiFetch<ArtworkDetail>(`/api/v1/artworks/${id}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  }

  if (artwork.status !== "PUBLISHED") {
    notFound();
  }

  const media = await Promise.all(
    artwork.mediaIds.map((mediaId) => apiFetch<PublicMedia>(`/api/v1/media/${mediaId}`))
  );
  const galleryItems: MediaGalleryItem[] = media.map((item, index) => ({
    id: item.id,
    src: item.url,
    alt: index === 0 ? artwork.title : `${artwork.title} 细节 ${index}`,
    aspect: index === 0 ? "4/5" : "1/1"
  }));
  const price = new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: artwork.priceCurrency
  }).format(Number(artwork.priceAmount));

  return (
    <div className="mx-auto w-full max-w-gallery px-6 py-16">
      <Link href={ROUTES.home} className="mb-8 inline-flex min-h-11 items-center text-sm text-ink-500 hover:text-ink-900">
        返回发现
      </Link>
      <div className="grid gap-10 lg:grid-cols-[3fr_2fr]">
        <MediaGallery items={galleryItems} />
        <aside className="flex flex-col gap-6">
          <div>
            <p className="text-sm text-ink-500">作品</p>
            <h1 className="mt-1 text-2xl text-ink-900 md:text-3xl">{artwork.title}</h1>
            <p className="mt-2 text-sm text-ink-500">已发布作品</p>
          </div>

          <div className="rounded-token border border-ink-300 p-4">
            <p className="text-sm text-ink-500">价格</p>
            <p className="mt-1 text-2xl text-ink-900">
              {artwork.stock > 0 ? price : "非卖品"}
            </p>
            <div className="mt-4 flex flex-col gap-2">
              {/* Week 2 占位操作：尚未创建订单、收藏或分享副作用。 */}
              <Link href={`/checkout/${artwork.id}`} className="inline-flex h-12 items-center justify-center rounded-token bg-ink-900 px-5 text-base font-medium text-white hover:bg-ink-700">立即购买</Link>
              <div className="grid grid-cols-2 gap-2">
                <FavoriteButton artworkId={artwork.id} initialFavorited={artwork.favoritedByMe} />
                <Button variant="secondary" leftIcon={<Share2 className="size-4" />}>
                  分享
                </Button>
              </div>
            </div>
            <p className="mt-3 text-xs text-ink-500">
              支付由 Stripe 安全处理；订单状态以 Webhook 通知为准。
            </p>
          </div>

          {artwork.creatorId ? (
            <Link
              href={ROUTES.creatorProfile(artwork.creatorId)}
              className="flex items-center gap-3 rounded-token border border-ink-300 p-4 hover:bg-ink-100"
            >
              <span className="relative size-12 overflow-hidden rounded-full bg-ink-100">
                <span className="flex size-full items-center justify-center text-sm text-ink-500">Creator</span>
              </span>
              <div className="flex flex-col">
                <span className="text-base text-ink-900">查看创作者</span>
                <span className="text-xs text-ink-500">进入创作者主页</span>
              </div>
            </Link>
          ) : null}
        </aside>
      </div>
      <section className="mt-16 max-w-2xl">
        <h2 className="text-xl text-ink-900">作品描述</h2>
        <p className="mt-4 whitespace-pre-wrap text-ink-700">{artwork.description}</p>
      </section>
    </div>
  );
}
