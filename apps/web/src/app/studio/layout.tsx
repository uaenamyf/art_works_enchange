import Link from "next/link";
import type { ReactNode } from "react";

/** Creator Studio 的信息密度布局；桌面显示侧栏，小屏保持顶部横向导航。 */
export default function StudioLayout({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto flex w-full max-w-[1440px] flex-col gap-8 px-5 py-8 lg:flex-row lg:px-8 lg:py-10">
      <aside className="w-full shrink-0 border-b border-ink-300 pb-5 lg:w-56 lg:border-b-0 lg:border-r lg:pb-0 lg:pr-6">
        <div className="mb-5">
          <p className="text-xs uppercase tracking-[0.18em] text-brand-600">Creator Studio</p>
          <p className="mt-2 text-sm text-ink-500">创作者工作台</p>
        </div>
        <nav aria-label="Creator Studio" className="flex gap-2 overflow-x-auto lg:flex-col">
          <Link href="/studio/dashboard" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">概览</Link>
          <Link href="/studio/artworks" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">作品管理</Link>
          <Link href="/studio/artworks/new" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">添加作品</Link>
          <Link href="/studio/orders" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">订单履约</Link>
          <Link href="/studio/settings/payments" className="flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">收款方式</Link>
        </nav>
      </aside>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}