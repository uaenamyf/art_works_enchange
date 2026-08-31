import Link from "next/link";
import type { ReactNode } from "react";

/** Admin 工作台布局；所有数据接口仍由后端 ADMIN 权限作为最终边界。 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto flex w-full max-w-[1440px] flex-col gap-8 px-5 py-8 lg:flex-row lg:px-8 lg:py-10">
      <aside className="w-full shrink-0 border-b border-ink-300 pb-5 lg:w-56 lg:border-b-0 lg:border-r lg:pb-0 lg:pr-6">
        <div className="mb-5">
          <p className="text-xs uppercase tracking-[0.18em] text-brand-600">Admin</p>
          <p className="mt-2 text-sm text-ink-500">平台运营后台</p>
        </div>
        <nav aria-label="管理后台" className="flex gap-2 overflow-x-auto lg:flex-col">
          <Link href="/admin" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">运营概览</Link>
          <Link href="/admin/artworks/review" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">作品审核</Link>
          <Link href="/admin/artworks/review-history" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">审核历史</Link>
        </nav>
      </aside>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}
