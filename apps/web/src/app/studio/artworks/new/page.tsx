import { StudioArtworkForm } from "@/features/artworks/StudioArtworkForm";
import { getServerLocale } from "@/features/locale/serverLocale";
import { translate } from "@/features/locale/translate";

/** Studio 新建作品页，承载草稿、媒体上传和审核提交的 Week 4 工作流。 */
export default async function NewStudioArtworkPage() {
  const locale = await getServerLocale();
  return (
    <main className="mx-auto w-full max-w-6xl px-6 py-12">
      <header className="mb-8 max-w-2xl">
        <p className="text-sm uppercase tracking-[0.18em] text-brand-600">Creator Studio</p>
        <h1 className="mt-2 font-serif text-4xl text-ink-900">{translate(locale, "addArtworkTitle")}</h1>
        <p className="mt-3 text-ink-500">{translate(locale, "addArtworkHint")}</p>
      </header>
      <StudioArtworkForm />
    </main>
  );
}