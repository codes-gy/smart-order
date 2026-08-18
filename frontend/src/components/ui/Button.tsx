import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "@/utils/cn";
import { Spinner } from "@/components/ui/Spinner";

export type ButtonVariant = "primary" | "secondary" | "outline" | "ghost" | "danger" | "unstyled";
export type ButtonSize = "sm" | "md" | "lg";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** true인 동안 클릭을 막고 스피너를 보여준다 (결제 등 초고동시성 중복 클릭 방지 용도) */
  isLoading?: boolean;
  loadingText?: string;
  fullWidth?: boolean;
}

const variantClass: Record<ButtonVariant, string> = {
  primary:
    "bg-primary text-primary-foreground hover:opacity-90 active:opacity-80 disabled:opacity-50",
  secondary:
    "bg-surface-muted text-foreground hover:opacity-90 active:opacity-80 disabled:opacity-50",
  outline:
    "border border-border bg-transparent text-foreground hover:bg-surface-muted disabled:opacity-50",
  ghost: "bg-transparent text-foreground hover:bg-surface-muted disabled:opacity-40",
  danger:
    "bg-danger text-danger-foreground hover:opacity-90 active:opacity-80 disabled:opacity-50",
  // 배경/글자색을 전부 className으로 직접 지정해야 할 때(예: 소셜 로그인 브랜드 컬러) 사용한다.
  // Tailwind는 같은 유틸리티가 충돌하면 클래스 문자열 순서가 아니라 스타일시트 생성 순서로
  // 우선순위가 정해지므로, variant 기본 bg/text와 override className을 함께 쓰면
  // 의도한 색이 반영되지 않을 수 있다 — 이 variant는 애초에 충돌할 클래스가 없다.
  unstyled: "disabled:opacity-50",
};

// 매장/모바일 주요 버튼은 최소 48x48 터치 영역을 확보한다 (PRD 7.3).
const sizeClass: Record<ButtonSize, string> = {
  sm: "h-10 min-w-10 px-3 text-sm rounded-lg",
  md: "h-12 min-w-12 px-4 text-base rounded-xl",
  lg: "h-14 min-w-14 px-6 text-lg rounded-xl",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    variant = "primary",
    size = "md",
    isLoading = false,
    loadingText,
    fullWidth = false,
    disabled,
    className,
    children,
    type = "button",
    ...rest
  },
  ref,
) {
  const isDisabled = disabled || isLoading;

  return (
    <button
      ref={ref}
      type={type}
      disabled={isDisabled}
      aria-busy={isLoading}
      className={cn(
        "inline-flex items-center justify-center gap-2 font-medium transition-colors",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        "disabled:cursor-not-allowed",
        variantClass[variant],
        sizeClass[size],
        fullWidth && "w-full",
        className,
      )}
      {...rest}
    >
      {isLoading ? (
        <>
          <Spinner className="h-4 w-4" label={loadingText ?? "처리 중"} />
          <span>{loadingText ?? children}</span>
        </>
      ) : (
        children
      )}
    </button>
  );
});
