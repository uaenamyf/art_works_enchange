import Image from "next/image";
import Link from "next/link";
import { ROUTES } from "@/lib/config";
import { cn } from "@/components/domain/types";
import type { CreatorCardData } from "@/components/domain/types";

export interface CreatorCardProps {
  creator: CreatorCardData;
  className?: string;
}

/**
 * 创作者摘要卡片，整卡导航到公开主页；封面承担可访问名称，头像在已有文本上下文中作为装饰图。
 */
export function CreatorCard({ creator, className }: CreatorCardProps) {
  return (
    <Link
      href={ROUTES.creatorProfile(creator.id)}
      className={cn(
        "group flex flex-col overflow-hidden rounded-token border border-ink-300 bg-white",
        "transition-shadow hover:shadow-card",
        className
      )}
    >
      <div className="relative aspect-[16/9] bg-ink-100">
        <Image
          src={creator.coverUrl}
          alt={`${creator.name} 封面`}
          fill
          sizes="(min-width: 1024px) 33vw, 100vw"
          className="object-cover transition-transform duration-200 group-hover:scale-[1.02]"
        />
      </div>
      <div className="flex items-start gap-3 p-4">
        <span
          aria-hidden
          className="relative -mt-8 size-12 overflow-hidden rounded-full border-2 border-white bg-ink-100 shadow-card"
        >
          <Image src={creator.avatarUrl} alt="" fill sizes="48px" className="object-cover" />
        </span>
        <div className="flex flex-col">
          <span className="text-base text-ink-900">{creator.name}</span>
          <span className="text-xs text-ink-500">{creator.handle} · {creator.location}</span>
        </div>
      </div>
    </Link>
  );
}
