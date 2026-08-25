import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { ROUTES } from "@/lib/config";

export default function NotFound() {
  return (
    <div className="mx-auto flex min-h-[60vh] w-full max-w-gallery flex-col items-center justify-center px-6 text-center">
      <p className="text-sm uppercase tracking-widest text-ink-500">404</p>
      <h1 className="mt-3 text-3xl text-ink-900">这里没有作品</h1>
      <p className="mt-3 max-w-md text-ink-700">
        你寻找的页面已经离开画廊了。回首页继续发现更多创作者。
      </p>
      <div className="mt-6">
        <Link href={ROUTES.home}>
          <Button>返回首页</Button>
        </Link>
      </div>
    </div>
  );
}
