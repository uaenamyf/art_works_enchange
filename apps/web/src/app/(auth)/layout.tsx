import Link from "next/link";
import { ROUTES } from "@/lib/config";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6 px-6 py-16">
      <Link
        href={ROUTES.home}
        className="text-sm text-ink-500 hover:text-ink-900"
      >
        ← 返回首页
      </Link>
      {children}
    </div>
  );
}
