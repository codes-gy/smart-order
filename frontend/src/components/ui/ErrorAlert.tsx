import { IconAlertTriangle } from "@/components/ui/icons";
import { Button } from "@/components/ui/Button";
import { cn } from "@/utils/cn";

export interface ErrorAlertProps {
  title?: string;
  description?: string;
  onRetry?: () => void;
  retryLabel?: string;
  className?: string;
  /** "inline": 리스트/카드 내부 삽입용, "page": 페이지 전체를 채우는 에러 화면용 */
  layout?: "inline" | "page";
}

/**
 * API 4xx/5xx, 네트워크 유실 등 에러 상태를 표시하는 공통 컴포넌트.
 * onRetry가 주어지면 재시도 버튼을 노출한다.
 */
export function ErrorAlert({
  title = "문제가 발생했어요",
  description = "잠시 후 다시 시도해주세요.",
  onRetry,
  retryLabel = "다시 시도",
  className,
  layout = "inline",
}: ErrorAlertProps) {
  return (
    <div
      role="alert"
      className={cn(
        "flex flex-col items-center gap-3 rounded-2xl border border-danger/30 bg-danger/5 text-center",
        layout === "page" ? "min-h-[60vh] justify-center px-6 py-16" : "px-4 py-6",
        className,
      )}
    >
      <IconAlertTriangle className="h-8 w-8 text-danger" />
      <div className="flex flex-col gap-1">
        <p className="text-base font-semibold text-foreground">{title}</p>
        <p className="text-sm text-foreground/70">{description}</p>
      </div>
      {onRetry && (
        <Button variant="danger" size="sm" onClick={onRetry}>
          {retryLabel}
        </Button>
      )}
    </div>
  );
}
