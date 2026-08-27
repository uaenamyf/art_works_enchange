import { Skeleton } from "@/components/ui/Skeleton";

/** 全局路由加载态：保留作品网格结构，避免页面跳动或整页 spinner。 */
export default function Loading() {
  return (
    <div className="mx-auto w-full max-w-gallery px-6 py-16" aria-label="正在加载作品">
      <Skeleton className="mb-10 h-10 max-w-sm" />
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} aspect="4/5" />
        ))}
      </div>
    </div>
  );
}