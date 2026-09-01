"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { useAuth } from "@/features/auth/AuthProvider";
import { useLocale } from "@/features/locale/LocaleProvider";

/** Creator Studio 的信息密度布局；桌面显示侧栏，小屏保持顶部横向导航。 */
export default function StudioLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { session, loading } = useAuth();
  const { t } = useLocale();
  const isCreator = session?.user.role === "CREATOR";
  const linkClass = (path: string) =>
    `flex min-h-11 shrink-0 items-center rounded-token px-3 text-sm ${pathname === path ? "bg-ink-900 text-white" : "text-ink-700 hover:bg-ink-100 hover:text-ink-900"}`;

  return (
    <div className="mx-auto flex w-full max-w-[1440px] flex-col gap-8 px-5 py-8 lg:flex-row lg:px-8 lg:py-10">
      <aside className="w-full shrink-0 border-b border-ink-300 pb-5 lg:w-56 lg:border-b-0 lg:border-r lg:pb-0 lg:pr-6">
        <div className="mb-5">
          <p className="text-xs uppercase tracking-[0.18em] text-brand-600">Creator Studio</p>
          <p className="mt-2 text-sm text-ink-500">{t("creatorWorkspace")}</p>
        </div>
        <nav aria-label="Creator Studio" className="flex gap-2 overflow-x-auto lg:flex-col">
          {!loading && !isCreator ? <Link href="/studio/creator/apply" className={linkClass("/studio/creator/apply")}>{t("becomeCreator")}</Link> : null}
          {isCreator ? <>
            <Link href="/studio/dashboard" className={linkClass("/studio/dashboard")}>{t("overview")}</Link>
            <Link href="/studio/artworks" className={linkClass("/studio/artworks")}>{t("artworkManagement")}</Link>
            <Link href="/studio/artworks/new" className={linkClass("/studio/artworks/new")}>{t("listArtwork")}</Link>
            <Link href="/studio/orders" className={linkClass("/studio/orders")}>{t("fulfillment")}</Link>
            <Link href="/studio/settings/payments" className={linkClass("/studio/settings/payments")}>{t("paymentMethods")}</Link>
          </> : null}
        </nav>
      </aside>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}