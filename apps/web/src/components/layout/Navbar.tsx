"use client";

import Link from "next/link";
import { ChevronDown, Search, UserRound } from "lucide-react";
import { APP_NAME, ROUTES } from "@/lib/config";
import { useAuth } from "@/features/auth/AuthProvider";
import { productSections } from "@/features/artworks/artworkCatalog";

/**
 * 全站导航根据内存认证状态显示用户、Creator 入口与注销动作。
 */
export function Navbar() {
  const { session, loading, logout } = useAuth();

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
          <li>
            <div className="group relative">
              <Link
                href={ROUTES.discover}
                className="flex h-11 items-center gap-1 rounded-full px-4 hover:bg-ink-100 hover:text-ink-900 focus-visible:bg-ink-100"
                aria-haspopup="true"
              >
                发现
                <ChevronDown aria-hidden className="size-3.5 transition-transform group-hover:rotate-180 group-focus-within:rotate-180" />
              </Link>
              <div className="invisible fixed left-5 right-5 top-16 w-auto translate-x-0 translate-y-2 border-t border-ink-300 bg-white px-6 py-7 opacity-0 shadow-pop transition-all duration-200 group-hover:visible group-hover:translate-y-0 group-hover:opacity-100 group-focus-within:visible group-focus-within:translate-y-0 group-focus-within:opacity-100 lg:absolute lg:left-1/2 lg:right-auto lg:top-full lg:w-[min(720px,calc(100vw-40px))] lg:-translate-x-1/2">
                <div className="grid gap-x-10 gap-y-6 sm:grid-cols-3">
                  <div className="sm:col-span-1">
                    <p className="text-xs uppercase tracking-[0.18em] text-ink-500">探索作品</p>
                    <Link href={ROUTES.discover} className="mt-3 block text-xl text-ink-900 hover:text-brand-500">
                      全部作品
                    </Link>
                  </div>
                  <div className="sm:col-span-2">
                    <p className="text-xs uppercase tracking-[0.18em] text-ink-500">按产品类型浏览</p>
                    <div className="mt-3 grid gap-5 sm:grid-cols-2">
                      {productSections.map((section) => (
                        <div key={section.value}>
                          <Link href={`/categories/${section.categories[0][1]}`} className="text-base text-ink-900 hover:text-brand-500">
                            {section.label}
                          </Link>
                          <div className="mt-2 grid gap-y-1">
                            {section.categories.map(([label, value]) => (
                              <Link key={value} href={`/categories/${value}`} className="py-1 text-sm text-ink-700 hover:text-brand-500">
                                {label}
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
          <li className="hidden lg:block">
            <Link href="/creators" className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">
              创作者
            </Link>
          </li>
          <li>
            <Link href={ROUTES.discover} aria-label="搜索作品" className="flex size-11 items-center justify-center rounded-full hover:bg-ink-100 hover:text-ink-900">
              <Search aria-hidden className="size-4" />
            </Link>
          </li>
          {!loading && session ? (
            <>
              <li className="hidden lg:block">
                <span className="flex h-11 items-center gap-2 px-3 text-ink-500">
                  <UserRound aria-hidden className="size-4" />
                  {session.user.displayName}
                </span>
              </li>
              {session.user.role === "USER" ? (
                <li>
                  <Link href={ROUTES.creatorApply} className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">
                    申请 Creator
                  </Link>
                </li>
              ) : null}
              <li><Link href={ROUTES.favorites} className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">收藏</Link></li>
              <li><Link href={ROUTES.follows} className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">关注</Link></li>
              <li><Link href={ROUTES.orders} className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">订单</Link></li>
              <li><Link href={ROUTES.notifications} className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">通知</Link></li>
              <li>
                <button type="button" className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900" onClick={() => void logout()}>
                  注销
                </button>
              </li>
            </>
          ) : !loading ? (
            <>
              <li>
                <Link href={ROUTES.authLogin} className="flex h-11 items-center rounded-full px-4 hover:bg-ink-100 hover:text-ink-900">
                  登录
                </Link>
              </li>
              <li>
                <Link
                  href={ROUTES.authRegister}
                  className="flex h-11 items-center rounded-full bg-ink-900 px-4 text-white hover:bg-ink-700"
                >
                  注册
                </Link>
              </li>
            </>
          ) : null}
        </ul>
      </nav>
    </header>
  );
}
