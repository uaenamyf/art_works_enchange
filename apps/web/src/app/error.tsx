"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { ROUTES } from "@/lib/config";
import { humanizeProblem, type ProblemDetails } from "@/lib/api/problem";

export interface ErrorPageProps {
  error: Error & { digest?: string };
  reset: () => void;
}

/**
 * App Router 全局客户端错误边界：展示友好降级并允许用户触发 reset 重试。
 * 生产环境不输出原始异常；后续监控只上报脱敏 digest 等关联信息。
 */
export default function ErrorPage({ error, reset }: ErrorPageProps) {
  useEffect(() => {
    // 只在显式应用开发环境打印；不依赖 Next.js 自动设置的 NODE_ENV。
    if (process.env.NEXT_PUBLIC_APP_ENV === "development") {
      console.error("App error:", error);
    }
  }, [error]);

  return (
    <div className="mx-auto flex min-h-[60vh] w-full max-w-gallery flex-col items-center justify-center px-6 text-center">
      <p className="text-sm uppercase tracking-widest text-ink-500">500</p>
      <h1 className="mt-3 text-3xl text-ink-900">画廊暂时关闭了</h1>
      <p className="mt-3 max-w-md text-ink-700">
        {humanizeProblem(parseProblem(error))}
      </p>
      <div className="mt-6 flex gap-3">
        <Button onClick={reset}>重试</Button>
        <Link href={ROUTES.home}>
          <Button variant="secondary">返回首页</Button>
        </Link>
      </div>
    </div>
  );
}

function parseProblem(error: Error): ProblemDetails | null {
  // Next.js 错误对象不保证含完整 Problem Details；这里只提取稳定业务码，
  // 不反射任意字段，也不把堆栈或内部异常详情展示给用户。
  const maybeCode = (error as unknown as { code?: unknown }).code;
  if (maybeCode !== undefined && maybeCode !== null) {
    return { code: String(maybeCode), detail: error.message };
  }
  return null;
}
