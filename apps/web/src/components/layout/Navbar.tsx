import Link from "next/link";
import { APP_NAME, ROUTES } from "@/lib/config";

/**
 * 全站导航。Week 1-2 阶段用占位登录入口；接入 Identity 后再替换为真实按钮。
 */
export function Navbar() {
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
        <ul className="hidden items-center gap-6 text-sm text-ink-700 md:flex">
          <li>
            <Link href={ROUTES.discover} className="hover:text-ink-900">
              发现
            </Link>
          </li>
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
              创作者入驻
            </Link>
          </li>
        </ul>
      </nav>
    </header>
  );
}
