"use client";

import Image from "next/image";
import Link from "next/link";
import { Heart } from "lucide-react";
import { formatPrice, formatRelative } from "@/lib/format/format";
import { ROUTES } from "@/lib/config";
import { cn } from "@/components/domain/types";
import type { ArtworkCardData } from "@/components/domain/types";
import { useLocale } from "@/features/locale/LocaleProvider";

export interface ArtworkCardProps {
  artwork: ArtworkCardData;
  className?: string;
  priority?: boolean;
}

/**
 * 作品摘要卡片，展示销售状态、价格和发布时间，并分别导航到作品与创作者页面。
 * 心形只表达已收藏状态，不在卡片内部发起收藏 mutation。
 */
export function ArtworkCard({ artwork, className, priority }: ArtworkCardProps) {
  const { locale } = useLocale();
  const english = locale === "en-US";
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
        style={{ aspectRatio: artwork.aspect, display: "block", width: "100%" }}
        aria-label={`${english ? "View artwork" : "查看作品"}: ${artwork.title}`}
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
            aria-label={english ? "Favorited" : "已收藏"}
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
            {artwork.isForSale ? formatPrice(artwork.priceCents, artwork.currency) : english ? "Not for sale" : "非卖品"}
          </span>
          <time className="text-ink-500" dateTime={artwork.publishedAt}>
            {formatRelative(artwork.publishedAt, english ? "en-US" : "zh-CN")}
          </time>
        </div>
      </div>
    </article>
  );
}
