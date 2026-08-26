import { StudioArtworkEditor } from "@/features/artworks/StudioArtworkEditor";

interface PageProps {
  params: Promise<{ id: string }>;
}

/** Studio 作品编辑页；草稿查询使用 Creator 专用端点，不暴露到公共详情接口。 */
export default async function StudioArtworkPage({ params }: PageProps) {
  const { id } = await params;
  return (
    <main className="mx-auto w-full max-w-6xl px-6 py-12">
      <header className="mb-8 max-w-2xl">
        <p className="text-sm uppercase tracking-[0.18em] text-brand-600">Creator Studio</p>
        <h1 className="mt-2 font-serif text-4xl text-ink-900">编辑作品</h1>
        <p className="mt-3 text-ink-500">修改内容后保存草稿，确认图片完整后再提交审核。</p>
      </header>
      <StudioArtworkEditor artworkId={id} />
    </main>
  );
}