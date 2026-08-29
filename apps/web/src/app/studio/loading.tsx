/** Creator Studio 路由加载边界，保持与最终内容相近的布局尺寸。 */
export default function StudioLoading() {
  return (
    <main className="mx-auto w-full max-w-[1440px] px-5 py-10 lg:px-8">
      <div role="status" className="animate-pulse space-y-4">
        <div className="h-3 w-28 rounded bg-ink-100" />
        <div className="h-10 w-56 rounded bg-ink-100" />
        <div className="h-5 w-full max-w-xl rounded bg-ink-100" />
      </div>
    </main>
  );
}