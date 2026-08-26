import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Heart, Share2 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { ArtworkCard } from "@/components/domain/ArtworkCard";
import { MediaGallery, type MediaGalleryItem } from "@/components/domain/MediaGallery";
import { findArtwork, findArtworksByCreator, findCreator } from "@/lib/mock/data";
import { ROUTES } from "@/lib/config";
import { formatPrice, formatDate } from "@/lib/format/format";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function ArtworkDetailPage({ params }: PageProps) {
  const { id } = await params;
  const artwork = findArtwork(id);
  if (!artwork) {
    notFound();
  }
  const creator = findCreator(artwork.creatorId);
  const moreFromCreator = findArtworksByCreator(artwork.creatorId)
    .filter((a) => a.id !== artwork.id)
    .slice(0, 4);

  const galleryItems: MediaGalleryItem[] = [
    { id: "m-1", src: artwork.imageUrl, alt: artwork.title, aspect: "4/5" },
    { id: "m-2", src: artwork.imageUrl, alt: `${artwork.title} 细节 1`, aspect: "1/1" },
    { id: "m-3", src: artwork.imageUrl, alt: `${artwork.title} 细节 2`, aspect: "1/1" }
  ];

  return (
    <div className="mx-auto w-full max-w-gallery px-6 py-16">
      <div className="grid gap-10 lg:grid-cols-[3fr_2fr]">
        <MediaGallery items={galleryItems} />
        <aside className="flex flex-col gap-6">
          <div>
            <p className="text-sm text-ink-500">作品</p>
            <h1 className="mt-1 text-2xl text-ink-900 md:text-3xl">{artwork.title}</h1>
            <p className="mt-2 text-sm text-ink-500">
              <time dateTime={artwork.publishedAt}>发布于 {formatDate(artwork.publishedAt)}</time>
            </p>
          </div>

          <div className="rounded-token border border-ink-300 p-4">
            <p className="text-sm text-ink-500">价格</p>
            <p className="mt-1 text-2xl text-ink-900">
              {artwork.isForSale ? formatPrice(artwork.priceCents, artwork.currency) : "非卖品"}
            </p>
            <div className="mt-4 flex flex-col gap-2">
              <Button size="lg">立即购买</Button>
              <div className="grid grid-cols-2 gap-2">
                <Button variant="secondary" leftIcon={<Heart className="size-4" />}>
                  {artwork.isFavorite ? "已收藏" : "收藏"}
                </Button>
                <Button variant="secondary" leftIcon={<Share2 className="size-4" />}>
                  分享
                </Button>
              </div>
            </div>
            <p className="mt-3 text-xs text-ink-500">
              支付由 Stripe 安全处理；订单状态以 Webhook 通知为准。
            </p>
          </div>

          {creator ? (
            <Link
              href={ROUTES.creatorProfile(creator.id)}
              className="flex items-center gap-3 rounded-token border border-ink-300 p-4 hover:bg-ink-100"
            >
              <span className="relative size-12 overflow-hidden rounded-full bg-ink-100">
                <Image src={creator.avatarUrl} alt="" fill sizes="48px" className="object-cover" />
              </span>
              <div className="flex flex-col">
                <span className="text-base text-ink-900">{creator.name}</span>
                <span className="text-xs text-ink-500">{creator.handle}</span>
              </div>
            </Link>
          ) : null}
        </aside>
      </div>

      {moreFromCreator.length > 0 ? (
        <section className="mt-16">
          <h2 className="text-xl text-ink-900">该创作者的其他作品</h2>
          <ul className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {moreFromCreator.map((a) => (
              <li key={a.id}>
                <ArtworkCard artwork={a} />
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );
}
