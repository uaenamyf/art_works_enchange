import Image from "next/image";
import Link from "next/link";
import { Heart } from "lucide-react";
import { formatPrice, formatRelative } from "@/lib/format/format";
import { ROUTES } from "@/lib/config";
import { cn } from "@/components/domain/types";
import type { ArtworkCardData } from "@/components/domain/types";

export interface ArtworkCardProps {
  artwork: ArtworkCardData;
  className?: string;
  priority?: boolean;
}

export function ArtworkCard({ artwork, className, priority }: ArtworkCardProps) {
  return (
    <article
      className={cn(
        "group flex flex-col gap-3",
        "transition-transform hover:-translate-y-0.5",
        className
      )}
    >
      <Link
        href={ROUTES.artworkDetail(artwork.id)}
        className="relative block overflow-hidden rounded-token bg-ink-100"
        style={{ aspectRatio: artwork.aspect }}
        aria-label={`查看作品：${artwork.title}`}
      >
        <Image
          src={artwork.imageUrl}
          alt={artwork.title}
          fill
          sizes="(min-width: 1024px) 25vw, (min-width: 640px) 50vw, 100vw"
          className="object-cover transition-transform duration-200 group-hover:scale-[1.02]"
          priority={priority}
        />
        {artwork.isFavorite ? (
          <span
            aria-label="已收藏"
            className="absolute right-3 top-3 inline-flex h-8 w-8 items-center justify-center rounded-full bg-white/90 text-brand-500 shadow-card"
          >
            <Heart aria-hidden className="size-4 fill-current" />
          </span>
        ) : null}
      </Link>
      <div className="flex flex-col gap-1">
        <h3 className="line-clamp-1 text-base text-ink-900">{artwork.title}</h3>
        <Link
          href={ROUTES.creatorProfile(artwork.creatorId)}
          className="text-sm text-ink-500 hover:text-ink-900"
        >
          {artwork.creatorName}
        </Link>
        <div className="mt-1 flex items-center justify-between text-sm">
          <span className="text-ink-900">
            {artwork.isForSale ? formatPrice(artwork.priceCents, artwork.currency) : "非卖品"}
          </span>
          <time className="text-ink-500" dateTime={artwork.publishedAt}>
            {formatRelative(artwork.publishedAt)}
          </time>
        </div>
      </div>
    </article>
  );
}
