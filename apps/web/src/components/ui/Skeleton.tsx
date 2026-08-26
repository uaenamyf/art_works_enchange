import { cn } from "@/components/domain/types";

export interface SkeletonProps {
  className?: string;
  /** 4/5 / 16/9 / 1/1 等 CSS aspect-ratio 值 */
  aspect?: "4/5" | "16/9" | "1/1";
}

/** 纯视觉加载占位；不承载业务状态，并通过 aria-hidden 对辅助技术隐藏。 */
export function Skeleton({ className, aspect }: SkeletonProps) {
  return (
    <div
      className={cn(
        "animate-pulse rounded-token bg-ink-100",
        aspect && `aspect-[${aspect}]`,
        className
      )}
      aria-hidden
    />
  );
}
