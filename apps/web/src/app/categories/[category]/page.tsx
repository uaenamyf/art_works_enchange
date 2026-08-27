import Link from "next/link";
import { notFound } from "next/navigation";
import { artworkCategories, ArtworkRow, getArtworkCategory, loadArtworkCards } from "@/features/artworks/artworkCatalog";

interface PageProps {
  params: Promise<{ category: string }>;
  searchParams: Promise<{ query?: string; cursor?: string }>;
}

/** 共享分类页：所有作品类别复用同一页面模板，只通过 URL 读取不同数据。 */
export default async function ArtworkCategoryPage({ params, searchParams }: PageProps) {
  const { category: rawCategory } = await params;
  const category = getArtworkCategory(rawCategory.toUpperCase());
  if (!category) notFound();
  const filters = await searchParams;
  const query = filters.query?.trim() ?? "";
  const response = await loadArtworkCards({ category: category[1], query, cursor: filters.cursor });
  return (
    <main className="mx-auto w-full max-w-[1280px] px-5 pb-20 pt-16 lg:px-8 lg:pt-24">
      <header className="mb-12 max-w-3xl">
        <Link href="/" className="text-sm text-ink-500 hover:text-ink-900">返回首页</Link>
        <p className="mt-8 text-xs uppercase tracking-[0.22em] text-brand-500">CreatorHub / 分类作品</p>
        <h1 className="mt-4 text-5xl leading-[1.05] text-ink-900 md:text-7xl">{category[0]}</h1>
        <p className="mt-5 max-w-xl text-base leading-7 text-ink-700">浏览这一类别中正在发生的独立创作。</p>
      </header>
      <form method="get" className="mb-10 flex max-w-2xl flex-col gap-3 border-y border-ink-300 py-4 sm:flex-row sm:items-end">
        <label className="flex flex-1 flex-col gap-1.5 text-sm text-ink-700">搜索此分类<input name="query" defaultValue={query} placeholder="输入标题或描述" className="h-11 rounded-token border border-ink-300 bg-white px-3 focus-visible:border-ink-900 focus-visible:outline-none" /></label>
        <button type="submit" className="h-11 rounded-token bg-ink-900 px-5 text-sm font-medium text-white hover:bg-ink-700">搜索</button>
      </form>
      <nav aria-label="作品分类" className="mb-12 flex gap-2 overflow-x-auto pb-2">
        {artworkCategories.map(([label, value]) => <Link key={value} href={`/categories/${value}`} className={`shrink-0 rounded-full px-4 py-2 text-sm ${value === category[1] ? "bg-ink-900 text-white" : "border border-ink-300 text-ink-700 hover:border-ink-900"}`}>{label}</Link>)}
      </nav>
      <ArtworkRow items={response.items} nextCursor={response.nextCursor} query={query} category={category[1]} />
    </main>
  );
}