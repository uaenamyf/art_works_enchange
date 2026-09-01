import Link from "next/link";
import { notFound } from "next/navigation";
import { Search } from "lucide-react";
import { ArtworkRow, getArtworkCategory, loadArtworkCards } from "@/features/artworks/artworkCatalog";
import { getServerLocale } from "@/features/locale/serverLocale";

interface PageProps {
  params: Promise<{ category: string }>;
  searchParams: Promise<{ query?: string; cursor?: string }>;
}

/** 共享分类页：所有作品类别复用同一页面模板，只通过 URL 读取不同数据。 */
export default async function ArtworkCategoryPage({ params, searchParams }: PageProps) {
  const { category: rawCategory } = await params;
  const category = getArtworkCategory(rawCategory.toUpperCase());
  if (!category) notFound();
  const locale = await getServerLocale();
  const english = locale === "en-US";
  const filters = await searchParams;
  const query = filters.query?.trim() ?? "";
  const response = await loadArtworkCards({ category: category[1], query, cursor: filters.cursor });
  return (
    <main className="mx-auto w-full max-w-[1280px] px-5 pb-20 pt-8 lg:px-8 lg:pt-12">
      <header className="mb-8 max-w-3xl">
        <Link href="/" className="text-sm text-ink-500 hover:text-ink-900">{english ? "Back home" : "返回首页"}</Link>
        <p className="mt-5 text-xs uppercase tracking-[0.22em] text-brand-500">CreatorHub / {english ? "Category" : "分类作品"}</p>
        <h1 className="mt-3 text-5xl leading-[1.05] text-ink-900 md:text-7xl">{category[0]}</h1>
        <p className="mt-4 max-w-xl text-base leading-7 text-ink-700">{english ? "Browse independent creations in this category." : "浏览这一类别中正在发生的独立创作。"}</p>
      </header>
      <form method="get" className="mb-8 flex max-w-3xl items-center gap-3 rounded-full border border-ink-300 bg-white px-4 py-1.5 shadow-card transition-colors focus-within:border-ink-900 focus-within:shadow-pop">
        <Search aria-hidden className="size-4 shrink-0 text-ink-500" />
        <label className="min-w-0 flex-1">
          <span className="sr-only">{english ? "Search this category" : "搜索此分类"}</span>
          <input name="query" defaultValue={query} placeholder={english ? "Search artwork titles or descriptions" : "搜索艺术品标题或描述"} className="h-10 w-full min-w-0 bg-transparent text-sm text-ink-900 outline-none placeholder:text-ink-500" />
        </label>
        {query ? <Link href={`/categories/${category[1]}`} aria-label={english ? "Clear search" : "清除搜索"} className="shrink-0 px-2 text-sm text-ink-500 hover:text-ink-900">{english ? "Clear" : "清除"}</Link> : null}
        <button type="submit" aria-label={english ? "Submit search" : "提交搜索"} className="flex h-10 shrink-0 items-center justify-center rounded-full bg-ink-900 px-5 text-sm font-medium text-white transition-colors hover:bg-ink-700">{english ? "Search" : "搜索"}</button>
      </form>
      <ArtworkRow items={response.items} nextCursor={response.nextCursor} query={query} category={category[1]} layout="grid" />
    </main>
  );
}