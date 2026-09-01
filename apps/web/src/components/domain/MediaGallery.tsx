"use client";

import Image from "next/image";
import { useState } from "react";
import { cn } from "@/components/domain/types";
import { useLocale } from "@/features/locale/LocaleProvider";

export interface MediaGalleryItem {
  id: string;
  src: string;
  alt: string;
  /** "4/5" / "16/9" / "1/1"，未提供时默认 4/5。 */
  aspect?: "4/5" | "16/9" | "1/1";
}

export interface MediaGalleryProps {
  items: MediaGalleryItem[];
  className?: string;
}

/**
 * 媒体画廊：左侧主图（4:5），右侧缩略图列表。
 * 暂为骨架/静态版，后续接 OpenAPI 媒体接口时，
 * 只需把 items 替换为远端数据。
 */
export function MediaGallery({ items, className }: MediaGalleryProps) {
  const { locale } = useLocale();
  const english = locale === "en-US";
  const [activeId, setActiveId] = useState<string>(items[0]?.id ?? "");
  const active = items.find((item) => item.id === activeId) ?? items[0];

  if (!active) {
    return <div className={cn("rounded-token bg-ink-100 p-8 text-ink-500", className)}>{english ? "No media" : "暂无媒体"}</div>;
  }

  return (
    <div className={cn("grid gap-4 md:grid-cols-[2fr_1fr]", className)}>
      <div
        className="relative overflow-hidden rounded-token bg-ink-100"
        style={{ aspectRatio: active.aspect ?? "4/5" }}
      >
        <Image
          src={active.src}
          alt={active.alt}
          fill
          sizes="(min-width: 768px) 60vw, 100vw"
          className="object-cover"
          priority
        />
      </div>
      <ul className="grid grid-cols-3 gap-2 md:grid-cols-1">
        {items.map((item) => {
          const selected = item.id === activeId;
          return (
            <li key={item.id}>
              <button
                type="button"
                onClick={() => setActiveId(item.id)}
                aria-pressed={selected}
                aria-label={`${english ? "View media" : "查看媒体"}: ${item.alt}`}
                className={cn(
                  "relative block w-full overflow-hidden rounded-token border",
                  "aspect-square",
                  selected ? "border-ink-900" : "border-ink-300 hover:border-ink-700"
                )}
              >
                <Image
                  src={item.src}
                  alt=""
                  fill
                  sizes="120px"
                  className="object-cover"
                />
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
