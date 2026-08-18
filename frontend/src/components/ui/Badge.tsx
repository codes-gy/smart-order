import type { ReactNode } from "react";
import { cn } from "@/utils/cn";

export type BadgeVariant = "neutral" | "success" | "warning" | "danger" | "accent";

export interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  className?: string;
}

/** success/warning/danger/accent는 밝은 배경(/15 tint) 위 텍스트이므로
 * 버튼 등에 쓰이는 원색(--success 등)이 아니라 대비를 확보한 -text 토큰을 사용한다 (WCAG AA). */
const variantClass: Record<BadgeVariant, string> = {
  neutral: "bg-surface-muted text-foreground",
  success: "bg-success/15 text-success-text",
  warning: "bg-warning/15 text-warning-text",
  danger: "bg-danger/15 text-danger-text",
  accent: "bg-accent/15 text-accent-text",
};

export function Badge({ children, variant = "neutral", className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold leading-none",
        variantClass[variant],
        className,
      )}
    >
      {children}
    </span>
  );
}
