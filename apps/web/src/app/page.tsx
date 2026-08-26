import { Tabs } from "@/components/ui/Tabs";
import { ArtworkCard } from "@/components/domain/ArtworkCard";
import { mockArtworks } from "@/lib/mock/data";
import type { ArtworkCardData } from "@/components/domain/types";

/**
 * Discover 页面：Week 1-2 阶段只渲染静态骨架 + mock 数据。
 * 视觉上强调留白与 4:5 作品比例，避免促销型电商模板。
 */
export default function DiscoverPage() {
  const latest: ArtworkCardData[] = [...mockArtworks].sort(
    (a, b) => +new Date(b.publishedAt) - +new Date(a.publishedAt)
  );
  const popular = [...mockArtworks].reverse();

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
            content: <ArtworkGrid items={popular} />
          },
          {
            value: "trending",
            label: "趋势",
            content: <ArtworkGrid items={popular.slice().reverse()} />
          }
        ]}
      />
    </div>
  );
}

function ArtworkGrid({
  items,
  priorityFirst = false
}: {
  items: ArtworkCardData[];
  priorityFirst?: boolean;
}) {
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
