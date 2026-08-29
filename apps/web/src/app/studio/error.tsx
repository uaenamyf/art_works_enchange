"use client";

/** Studio 路由错误边界；不向用户暴露服务端异常详情。 */
export default function StudioError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <main className="mx-auto w-full max-w-[960px] px-5 py-16 lg:px-8">
      <p role="alert" className="text-sm text-error">工作台暂时无法加载，请稍后重试。</p>
      <button type="button" className="mt-6 min-h-11 rounded-token border border-ink-900 px-5 text-sm text-ink-900" onClick={reset}>
        重试
      </button>
    </main>
  );
}