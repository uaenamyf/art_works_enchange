import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArtworkCard } from "@/components/domain/ArtworkCard";
import { findArtworksByCreator, findCreator } from "@/lib/mock/data";
import { ROUTES } from "@/lib/config";

interface PageProps {
  params: Promise<{ id: string }>;
}

/**
 * 创作者公开主页：按路由 ID 解析创作者，缺失时返回 404，并展示其公开作品。
 * “关注”目前是无状态占位；Week 6 必须通过 Follow 用例校验登录状态和禁止自关注规则。
 */
export default async function CreatorProfilePage({ params }: PageProps) {
  const { id } = await params;
  const creator = findCreator(id);
  if (!creator) {
    notFound();
  }
  const works = findArtworksByCreator(creator.id);

  return (
    <div>
      <div className="relative aspect-[16/9] w-full overflow-hidden bg-ink-100">
        <Image
          src={creator.coverUrl}
          alt={`${creator.name} 封面`}
          fill
          sizes="100vw"
          className="object-cover"
          priority
        />
      </div>
      <div className="mx-auto w-full max-w-gallery px-6 py-16">
        <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="flex items-end gap-4">
            <span className="relative -mt-12 size-24 overflow-hidden rounded-full border-4 border-white bg-ink-100 shadow-card">
              <Image src={creator.avatarUrl} alt="" fill sizes="96px" className="object-cover" />
            </span>
            <div>
              <h1 className="text-2xl text-ink-900 md:text-3xl">{creator.name}</h1>
              <p className="text-sm text-ink-500">
                {creator.handle} · {creator.location}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="h-11 rounded-token border border-ink-900 px-4 text-sm text-ink-900 hover:bg-ink-900 hover:text-white"
            >
              关注
            </button>
            <Link
              href={ROUTES.home}
              className="text-sm text-ink-500 hover:text-ink-900"
            >
              ← 返回
            </Link>
          </div>
        </header>

        <section className="mt-8 grid gap-8 md:grid-cols-[2fr_1fr]">
          <p className="text-base text-ink-700">{creator.bio}</p>
          <dl className="grid grid-cols-2 gap-4 rounded-token border border-ink-300 p-4 text-sm">
            <div>
              <dt className="text-ink-500">关注者</dt>
              <dd className="mt-1 text-lg text-ink-900">{creator.followers.toLocaleString()}</dd>
            </div>
            <div>
              <dt className="text-ink-500">已发布</dt>
              <dd className="mt-1 text-lg text-ink-900">{creator.publishedArtworks}</dd>
            </div>
          </dl>
        </section>

        <section className="mt-12">
          <h2 className="text-xl text-ink-900">作品</h2>
          {works.length === 0 ? (
            <p className="mt-4 text-ink-500">这位创作者还没有发布作品。</p>
          ) : (
            <ul className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {works.map((artwork) => (
                <li key={artwork.id}>
                  <ArtworkCard artwork={artwork} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
