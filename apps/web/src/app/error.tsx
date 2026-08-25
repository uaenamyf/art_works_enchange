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

export default function ErrorPage({ error, reset }: ErrorPageProps) {
  useEffect(() => {
    // 在生产环境接入 Sentry 时把 error.digest 一并上报。
    // 当前 Week 1-2 阶段只在控制台记录。
    if (process.env.NODE_ENV !== "production") {
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
  // Next.js error 对象没有结构化 Problem Details 字段；保留接口便于后续客户端错误兜底。
  const maybeCode = (error as unknown as { code?: unknown }).code;
  if (maybeCode !== undefined && maybeCode !== null) {
    return { code: String(maybeCode), detail: error.message };
  }
  return null;
}
