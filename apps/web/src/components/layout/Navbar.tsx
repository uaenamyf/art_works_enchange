"use client";

import Link from "next/link";
import { ChevronDown, Search, UserRound } from "lucide-react";
import { APP_NAME, ROUTES } from "@/lib/config";
import { useAuth } from "@/features/auth/AuthProvider";

const artworkCategories = [
  ["艺术品", "ART"],
  ["摄影", "PHOTOGRAPHY"],
  ["陶艺", "CERAMICS"],
  ["木制品", "WOODWORK"],
  ["皮具", "LEATHER"],
  ["珠宝", "JEWELRY"],
  ["时装", "FASHION"],
  ["数字作品", "DIGITAL"],
  ["收藏品", "COLLECTIBLES"]
] as const;

/**
 * 全站导航根据内存认证状态显示用户、Creator 入口与注销动作。
 */
export function Navbar() {
  const { session, loading, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 border-b border-ink-300 bg-white/90 backdrop-blur">
      <nav
        aria-label="主导航"
        className="mx-auto flex h-16 w-full max-w-[1280px] items-center justify-between px-5 lg:px-8"
      >
        <Link
          href={ROUTES.home}
          className="text-base font-semibold tracking-tight text-ink-900"
        >
          {APP_NAME}
        </Link>
        <ul className="flex items-center gap-1 text-sm text-ink-700 md:gap-2">
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
              <div className="invisible absolute left-1/2 top-full w-[min(720px,calc(100vw-40px))] -translate-x-1/2 translate-y-2 border-t border-ink-300 bg-white px-6 py-7 opacity-0 shadow-pop transition-all duration-200 group-hover:visible group-hover:translate-y-0 group-hover:opacity-100 group-focus-within:visible group-focus-within:translate-y-0 group-focus-within:opacity-100">
                <div className="grid gap-x-10 gap-y-6 sm:grid-cols-3">
                  <div className="sm:col-span-1">
                    <p className="text-xs uppercase tracking-[0.18em] text-ink-500">探索作品</p>
                    <Link href={ROUTES.discover} className="mt-3 block text-xl text-ink-900 hover:text-brand-500">
                      全部作品
                    </Link>
                    <Link href="/creators" className="mt-2 block text-sm text-ink-700 hover:text-ink-900">
                      创作者目录
                    </Link>
                  </div>
                  <div className="sm:col-span-2">
                    <p className="text-xs uppercase tracking-[0.18em] text-ink-500">按类别浏览</p>
                    <div className="mt-3 grid grid-cols-2 gap-x-6 gap-y-2 sm:grid-cols-3">
                      {artworkCategories.map(([label, value]) => (
                        <Link key={value} href={`/?category=${value}`} className="py-1 text-sm text-ink-700 hover:text-brand-500">
                          {label}
                        </Link>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </li>
          <li className="hidden md:block">
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
