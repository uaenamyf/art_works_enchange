import Link from "next/link";
import { ArtworkRow, artworkCategories, loadArtworkCards } from "@/features/artworks/artworkCatalog";

/** 独立首页：展示品牌叙事和各类别预览，不承担分类页的筛选职责。 */
export default async function HomePage() {
  const response = await loadArtworkCards({ limit: 20 });

  return (
    <main className="mx-auto w-full max-w-[1280px] px-5 pb-20 pt-16 lg:px-8 lg:pt-24">
      <section className="mb-16 max-w-3xl">
        <p className="text-xs uppercase tracking-[0.22em] text-brand-500">CreatorHub / 独立创作</p>
        <h1 className="mt-4 text-5xl leading-[1.05] text-ink-900 md:text-7xl">发现正在发生的创作</h1>
        <p className="mt-6 max-w-xl text-base leading-7 text-ink-700">从一件作品开始，遇见它背后的人。</p>
      </section>

      <section aria-label="按类别浏览作品" className="space-y-16">
        {response.items.length === 0 ? (
          <div className="border-y border-ink-300 py-20 text-center">
            <p className="text-xs uppercase tracking-[0.18em] text-ink-500">作品目录</p>
            <h2 className="mt-3 text-3xl text-ink-900">新的创作即将到来</h2>
            <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-ink-500">
              作品通过审核后，会按照艺术品、木制品、摄影等类别出现在这里。
            </p>
            <Link href="/creators" className="mt-6 inline-flex text-sm text-ink-900 underline decoration-ink-300 underline-offset-4 hover:decoration-brand-500">
              先去认识创作者
            </Link>
          </div>
        ) : artworkCategories.map(([label, value]) => (
          <section key={value}>
            <div className="mb-5 flex items-end justify-between border-b border-ink-300 pb-4">
              <div><p className="text-xs uppercase tracking-[0.18em] text-ink-500">作品类别</p><h2 className="mt-1 text-3xl text-ink-900">{label}</h2></div>
              <Link href={`/categories/${value}`} className="text-sm text-ink-500 hover:text-ink-900">查看全部</Link>
            </div>
            <ArtworkRow items={response.items.filter((item) => item.category === value)} nextCursor={null} category={value} />
          </section>
        ))}
      </section>
    </main>
  );
}
