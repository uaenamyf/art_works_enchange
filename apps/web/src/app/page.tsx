import Link from "next/link";
import { ArtworkRow, getArtworkCategoryLabel, loadArtworkCards, productSections } from "@/features/artworks/artworkCatalog";
import { getServerLocale } from "@/features/locale/serverLocale";

/** 独立首页：展示品牌叙事和各类别预览，不承担分类页的筛选职责。 */
export default async function HomePage() {
  const locale = await getServerLocale();
  const english = locale === "en-US";
  const sections = await Promise.all(productSections.map(async (section) => ({
    ...section,
    response: await loadArtworkCards({ productType: section.value, limit: 20 })
  })));

  return (
    <main className="mx-auto w-full max-w-[1280px] px-5 pb-20 pt-16 lg:px-8 lg:pt-24">
      <section className="mb-16 max-w-3xl">
        <p className="text-xs uppercase tracking-[0.22em] text-brand-500">CreatorHub / {english ? "Independent creation" : "独立创作"}</p>
        <h1 className="mt-4 text-5xl leading-[1.05] text-ink-900 md:text-7xl">{english ? "Discover what is being created" : "发现正在发生的创作"}</h1>
        <p className="mt-6 max-w-xl text-base leading-7 text-ink-700">{english ? "Start with an artwork and meet the person behind it." : "从一件作品开始，遇见它背后的人。"}</p>
      </section>

      <section aria-label={english ? "Browse artworks by product type" : "按产品类型浏览作品"} className="space-y-16">
        {sections.map((section) => (
          <ProductSection key={section.value} section={section} english={english} />
        ))}
      </section>
    </main>
  );
}

function ProductSection({ section, english }: { section: (typeof productSections)[number] & { response: Awaited<ReturnType<typeof loadArtworkCards>> }; english: boolean }) {
  return (
    <section className="group/product min-w-0 overflow-hidden border-y border-ink-300 py-6" tabIndex={0} aria-label={`${english ? section.value === "DIGITAL" ? "Digital products" : "Physical products" : section.label} ${english ? "artworks" : "作品"}`}>
      <div className="flex flex-wrap items-end justify-between gap-5">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-brand-500">{english ? "Product section" : "产品板块"}</p>
          <h2 className="mt-1 text-3xl text-ink-900">{english ? section.value === "DIGITAL" ? "Digital products" : "Physical products" : section.label}</h2>
        </div>
        <Link href={`/categories/${section.categories[0][1]}`} className="text-sm text-ink-500 hover:text-ink-900">{english ? "View all" : "查看全部"}</Link>
      </div>
      <nav aria-label={`${section.label}${english ? " categories" : "分类"}`} className="mt-4 flex gap-2 overflow-x-auto pb-1 opacity-70 transition-opacity group-hover/product:opacity-100 group-focus-within/product:opacity-100">
        {section.categories.map(([, value]) => <Link key={value} href={`/categories/${value}`} className="shrink-0 rounded-token border border-ink-300 px-3 py-2 text-sm text-ink-700 hover:border-ink-900 hover:text-ink-900">{getArtworkCategoryLabel(value, english)}</Link>)}
      </nav>
      {section.response.items.length === 0 ? <div className="py-12 text-sm text-ink-500">{english ? "No published artworks" : "暂无已发布作品"}</div> : section.categories.map(([, value]) => {
        const items = section.response.items.filter((item) => item.category === value);
        if (items.length === 0) return null;
        return <div key={value} className="mt-8"><div className="mb-4 flex items-end justify-between border-b border-ink-300 pb-3"><h3 className="text-xl text-ink-900">{getArtworkCategoryLabel(value, english)}</h3><Link href={`/categories/${value}`} className="text-sm text-ink-500 hover:text-ink-900">{english ? "View all" : "查看全部"}</Link></div><ArtworkRow items={items} nextCursor={null} category={value} /></div>;
      })}
    </section>
  );
}
