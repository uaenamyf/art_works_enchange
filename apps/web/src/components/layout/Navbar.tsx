"use client";

import Link from "next/link";
import { APP_NAME, ROUTES } from "@/lib/config";
import { useAuth } from "@/features/auth/AuthProvider";

/**
 * 全站导航根据内存认证状态显示用户、Creator 入口与注销动作。
 */
export function Navbar() {
  const { session, loading, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 border-b border-ink-300 bg-white/90 backdrop-blur">
      <nav
        aria-label="主导航"
        className="mx-auto flex h-14 w-full max-w-gallery items-center justify-between px-6"
      >
        <Link
          href={ROUTES.home}
          className="text-base font-semibold tracking-tight text-ink-900"
        >
          {APP_NAME}
        </Link>
        <ul className="flex items-center gap-3 text-sm text-ink-700 md:gap-6">
          <li>
            <Link href={ROUTES.discover} className="hover:text-ink-900">
              发现
            </Link>
          </li>
          {!loading && session ? (
            <>
              <li className="hidden text-ink-500 sm:list-item">{session.user.displayName}</li>
              {session.user.role === "USER" ? (
                <li>
                  <Link href={ROUTES.creatorApply} className="hover:text-ink-900">
                    申请 Creator
                  </Link>
                </li>
              ) : null}
              <li>
                <button type="button" className="hover:text-ink-900" onClick={() => void logout()}>
                  注销
                </button>
              </li>
            </>
          ) : !loading ? (
            <>
              <li>
                <Link href={ROUTES.authLogin} className="hover:text-ink-900">
                  登录
                </Link>
              </li>
              <li>
                <Link
                  href={ROUTES.authRegister}
                  className="rounded-token border border-ink-900 px-3 py-1.5 text-ink-900 hover:bg-ink-900 hover:text-white"
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
