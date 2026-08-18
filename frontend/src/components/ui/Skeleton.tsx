import { cn } from "@/utils/cn";

export interface SkeletonProps {
  className?: string;
}

/** 로딩 상태 표현용 기본 스켈레톤 블록 */
export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      aria-hidden="true"
      className={cn("animate-pulse rounded-lg bg-surface-muted", className)}
    />
  );
}

export interface SkeletonTextProps {
  lines?: number;
  className?: string;
  lastLineWidthClassName?: string;
}

/** 여러 줄 텍스트 로딩을 표현하는 스켈레톤 (카드/리스트 아이템 등에서 반복 사용) */
export function SkeletonText({
  lines = 2,
  className,
  lastLineWidthClassName = "w-2/3",
}: SkeletonTextProps) {
  return (
    <div className={cn("flex flex-col gap-2", className)} aria-hidden="true">
      {Array.from({ length: lines }).map((_, index) => (
        <Skeleton
          key={index}
          className={cn("h-4 w-full", index === lines - 1 && lastLineWidthClassName)}
        />
      ))}
    </div>
  );
}
