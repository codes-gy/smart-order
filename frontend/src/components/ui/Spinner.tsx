import { cn } from "@/utils/cn";

export interface SpinnerProps {
  className?: string;
  /** 스크린리더용 라벨. 컨텍스트가 있으면 더 구체적으로 넘겨줄 것 */
  label?: string;
}

export function Spinner({ className, label = "로딩 중" }: SpinnerProps) {
  return (
    <span role="status" className="inline-flex items-center">
      <svg
        className={cn("h-5 w-5 animate-spin text-current", className)}
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="4"
        />
        <path
          className="opacity-90"
          fill="currentColor"
          d="M4 12a8 8 0 0 1 8-8V0C5.37 0 0 5.37 0 12h4Z"
        />
      </svg>
      <span className="sr-only">{label}</span>
    </span>
  );
}
