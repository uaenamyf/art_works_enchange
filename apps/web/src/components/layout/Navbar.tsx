"use client";

import Link from "next/link";
import { useState } from "react";
import { ChevronDown, Globe2, UserRound } from "lucide-react";
import { APP_NAME, ROUTES } from "@/lib/config";
import { useAuth } from "@/features/auth/AuthProvider";
import { getArtworkCategoryLabel, getProductTypeLabel, productSections } from "@/features/artworks/artworkCatalog";
import { useLocale } from "@/features/locale/LocaleProvider";

/**
 * 全站导航根据内存认证状态显示用户、Creator 入口与注销动作。
 */
export function Navbar() {
  const { session, loading, logout } = useAuth();
  const { locale, setLocale, t } = useLocale();
  const [activeMenu, setActiveMenu] = useState<"discover" | "creators" | "account" | "language" | null>(null);

  // 顶层菜单共用一个活动状态，确保鼠标悬停与键盘焦点不会同时打开两个菜单。
  const menuHandlers = (menu: "discover" | "creators" | "account" | "language") => ({
    onMouseEnter: () => setActiveMenu(menu),
    onMouseLeave: () => setActiveMenu(null),
    onFocus: () => setActiveMenu(menu),
    onBlur: (event: React.FocusEvent<HTMLLIElement>) => {
      if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
        setActiveMenu(null);
      }
    },
  });

  return (
    <header className="sticky top-0 z-30 border-b border-ink-300 bg-white/90 backdrop-blur">
      <nav
        aria-label="主导航"
        className="mx-auto flex h-16 w-full min-w-0 max-w-[1280px] items-center justify-between overflow-visible px-5 lg:px-8"
      >
        <Link
          href={ROUTES.home}
          className="text-base font-semibold tracking-tight text-ink-900"
        >
          {APP_NAME}
        </Link>
        <ul className="flex min-w-0 max-w-[calc(100%-7rem)] items-center justify-end gap-1 overflow-x-auto text-sm text-ink-700 md:gap-2 lg:overflow-visible">
          <li className="group relative flex h-16 items-center" {...menuHandlers("discover")}>
            <div className="relative flex h-full items-center">
              <Link
                href={ROUTES.discover}
                onClick={() => setActiveMenu(null)}
                className="flex h-11 items-center gap-1 rounded-full px-4 hover:bg-ink-100 hover:text-ink-900 focus-visible:bg-ink-100"
                aria-haspopup="true"
                aria-expanded={activeMenu === "discover"}
              >
                {t("discover")}
                <ChevronDown aria-hidden className={`size-3.5 transition-transform ${activeMenu === "discover" ? "rotate-180" : ""}`} />
              </Link>
              <div className={`fixed left-5 right-5 top-16 w-auto border-t border-ink-300 bg-white px-6 py-7 shadow-pop transition-all duration-200 md:absolute md:left-1/2 md:right-auto md:top-full md:w-[min(720px,calc(100vw-40px))] md:-translate-x-1/2 ${activeMenu === "discover" ? "visible translate-y-0 opacity-100" : "invisible translate-y-2 opacity-0"}`}>
                <div className="grid gap-x-10 gap-y-6 sm:grid-cols-3">
                  <div className="sm:col-span-1">
                    <p className="text-xs uppercase tracking-[0.18em] text-ink-500">{t("exploreArtworks")}</p>
                    <Link href={ROUTES.discover} className="mt-3 block text-xl text-ink-900 hover:text-brand-500">
                      {t("allArtworks")}
                    </Link>
                  </div>
                  <div className="sm:col-span-2">
                    <p className="text-xs uppercase tracking-[0.18em] text-ink-500">{t("browseByType")}</p>
                    <div className="mt-3 grid gap-5 sm:grid-cols-2">
                      {productSections.map((section) => (
                        <div key={section.value}>
                          <Link href={`/categories/${section.categories[0]}`} className="text-base text-ink-900 hover:text-brand-500">
                            {getProductTypeLabel(section.value, locale)}
                          </Link>
                          <div className="mt-2 grid gap-y-1">
                            {section.categories.map((value) => (
                              <Link key={value} href={`/categories/${value}`} className="py-1 text-sm text-ink-700 hover:text-brand-500">
                                {getArtworkCategoryLabel(value, locale)}
                              </Link>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </li>
          <li className="group relative flex h-16 items-center" {...menuHandlers("creators")}>
            <div className="relative flex h-full items-center">
              <Link
                href="/creators"
                onClick={() => setActiveMenu(null)}
                className="flex h-11 items-center gap-1 rounded-full px-4 hover:bg-ink-100 hover:text-ink-900 focus-visible:bg-ink-100"
                aria-haspopup="true"
                aria-expanded={activeMenu === "creators"}
              >
                {t("creators")}
                <ChevronDown aria-hidden className={`size-3.5 transition-transform ${activeMenu === "creators" ? "rotate-180" : ""}`} />
              </Link>
              <div className={`fixed left-5 right-5 top-16 w-auto border-t border-ink-300 bg-white px-6 py-7 shadow-pop transition-all duration-200 md:absolute md:left-1/2 md:right-auto md:top-full md:w-80 md:-translate-x-1/2 ${activeMenu === "creators" ? "visible translate-y-0 opacity-100" : "invisible translate-y-2 opacity-0"}`}>
                <p className="text-xs uppercase tracking-[0.18em] text-ink-500">{t("creators")}</p>
                <Link href="/creators" className="mt-3 block text-xl text-ink-900 hover:text-brand-500">
                  {t("creators")}
                </Link>
                <div className="mt-5 border-t border-ink-300 pt-5">
                  <p className="text-xs uppercase tracking-[0.18em] text-ink-500">{t("creatorStudio")}</p>
                  <div className="mt-3 grid gap-1">
                    {!session || session.user.role === "USER" ? <Link href={session ? ROUTES.creatorApply : ROUTES.authRegister} className="flex min-h-11 items-center rounded-token bg-ink-900 px-3 text-sm text-white hover:bg-ink-700">{t("becomeCreator")}</Link> : null}
                    {session ? <>
                      <Link prefetch={false} href="/studio/dashboard" className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("overview")}</Link>
                      <Link prefetch={false} href="/studio/artworks" className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("artworkManagement")}</Link>
                      <Link prefetch={false} href="/studio/artworks/new" className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("listArtwork")}</Link>
                      <Link prefetch={false} href="/studio/orders" className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("fulfillment")}</Link>
                      <Link prefetch={false} href="/studio/settings/payments" className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("paymentMethods")}</Link>
                    </> : <p className="mt-3 border-t border-ink-300 pt-4 text-sm text-ink-500">{t("loginToStudio")}</p>}
                  </div>
                </div>
              </div>
            </div>
          </li>
          {session ? <li className="group relative flex h-16 items-center" {...menuHandlers("account")}>
                <div className="relative flex h-full items-center">
                <button
                  type="button"
                  className="flex h-11 items-center gap-2 rounded-full px-4 hover:bg-ink-100 hover:text-ink-900 focus-visible:bg-ink-100"
                  aria-haspopup="true"
                  aria-expanded={activeMenu === "account"}
                  onClick={() => setActiveMenu(activeMenu === "account" ? null : "account")}
                >
                  <UserRound aria-hidden className="size-4" />
                  <span className="max-w-32 truncate">{session.user.displayName}</span>
                  <ChevronDown aria-hidden className={`size-3.5 transition-transform ${activeMenu === "account" ? "rotate-180" : ""}`} />
                </button>
                <div className={`fixed left-5 right-5 top-16 w-auto border-t border-ink-300 bg-white px-6 py-6 shadow-pop transition-all duration-200 md:absolute md:left-auto md:right-0 md:top-full md:w-72 md:translate-x-0 ${activeMenu === "account" ? "visible translate-y-0 opacity-100" : "invisible translate-y-2 opacity-0"}`}>
                  <div className="border-b border-ink-300 pb-4">
                    <p className="text-xs uppercase tracking-[0.18em] text-ink-500">{t("account")}</p>
                    <p className="mt-2 truncate text-base text-ink-900">{session.user.displayName}</p>
                  </div>
                  <div className="grid gap-1 border-b border-ink-300 py-4">
                    <p className="px-3 pb-1 text-xs text-ink-500">{t("personalSpace")}</p>
                    <Link prefetch={false} href={ROUTES.orders} className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("orders")}</Link>
                    <Link prefetch={false} href={ROUTES.favorites} className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("favorites")}</Link>
                    <Link prefetch={false} href={ROUTES.follows} className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("follows")}</Link>
                    <Link prefetch={false} href={ROUTES.notifications} className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("notifications")}</Link>
                  </div>
                  {session.user.role === "ADMIN" ? <div className="border-b border-ink-300 py-4"><p className="px-3 pb-1 text-xs text-ink-500">{t("management")}</p><Link prefetch={false} href={ROUTES.admin} className="flex min-h-11 items-center rounded-token px-3 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900">{t("admin")}</Link></div> : null}
                  <button type="button" className="mt-4 flex min-h-11 w-full items-center rounded-token px-3 text-left text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900" onClick={() => void logout()}>
                    {t("logout")}
                  </button>
                </div>
              </div>
              </li> : null}
              <li className="group relative flex h-16 items-center" {...menuHandlers("language")}>
                <button
                  type="button"
                  aria-label={t("language")}
                  aria-haspopup="true"
                  aria-expanded={activeMenu === "language"}
                  onClick={() => setActiveMenu(activeMenu === "language" ? null : "language")}
                  className="flex h-11 items-center gap-2 rounded-full px-3 text-xs text-ink-700 hover:bg-ink-100 hover:text-ink-900 focus-visible:bg-ink-100"
                >
                  <Globe2 aria-hidden className="size-4" />
                  <span>{locale === "zh-CN" ? "中文" : "EN"}</span>
                  <ChevronDown aria-hidden className={`size-3.5 transition-transform ${activeMenu === "language" ? "rotate-180" : ""}`} />
                </button>
                <div role="menu" className={`fixed left-5 right-5 top-16 z-40 w-auto border-t border-ink-300 bg-white px-6 py-6 shadow-pop transition-all duration-200 md:absolute md:left-auto md:right-0 md:top-full md:w-72 md:translate-x-0 ${activeMenu === "language" ? "visible translate-y-0 opacity-100" : "invisible translate-y-2 opacity-0"}`}>
                    <p className="px-3 py-2 text-xs uppercase tracking-[0.18em] text-ink-500">{t("language")}</p>
                    <button type="button" role="menuitem" onClick={() => setLocale("zh-CN")} className={`flex min-h-11 w-full items-center rounded-token px-3 text-left text-sm hover:bg-ink-100 ${locale === "zh-CN" ? "bg-ink-100 text-ink-900" : "text-ink-700"}`}>
                      {t("chinese")}
                    </button>
                    <button type="button" role="menuitem" onClick={() => setLocale("en-US")} className={`flex min-h-11 w-full items-center rounded-token px-3 text-left text-sm hover:bg-ink-100 ${locale === "en-US" ? "bg-ink-100 text-ink-900" : "text-ink-700"}`}>
                      {t("english")}
                    </button>
                </div>
              </li>
          {!loading && !session ? (
            <>
              <li>
                <Link href={ROUTES.authLogin} className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">
                  {t("login")}
                </Link>
              </li>
              <li>
                <Link
                  href={ROUTES.authRegister}
                  className="flex h-11 items-center rounded-full bg-ink-900 px-4 text-white hover:bg-ink-700"
                >
                  {t("register")}
                </Link>
              </li>
            </>
          ) : null}
        </ul>
      </nav>
    </header>
  );
}
