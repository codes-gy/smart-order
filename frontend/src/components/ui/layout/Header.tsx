"use client";

import type { ReactNode } from "react";
import { IconChevronLeft } from "@/components/ui/icons";
import { cn } from "@/utils/cn";

export interface HeaderProps {
  title?: string;
  onBack?: () => void;
  rightSlot?: ReactNode;
  sticky?: boolean;
  className?: string;
}

/** 고객용 모바일 상단 앱바. onBack이 주어지면 좌측에 뒤로가기 버튼을 노출한다 */
export function Header({ title, onBack, rightSlot, sticky = true, className }: HeaderProps) {
  return (
    <header
      className={cn(
        "z-30 flex h-14 items-center gap-2 border-b border-border bg-background px-2",
        sticky && "sticky top-0",
        className,
      )}
    >
      <div className="flex w-12 shrink-0 items-center justify-start">
        {onBack && (
          <button
            type="button"
            onClick={onBack}
            aria-label="뒤로가기"
            className="flex h-12 w-12 items-center justify-center rounded-full hover:bg-surface-muted"
          >
            <IconChevronLeft />
          </button>
        )}
      </div>
      <h1 className="min-w-0 flex-1 truncate text-center text-base font-semibold text-foreground">
        {title}
      </h1>
      <div className="flex min-w-12 shrink-0 items-center justify-end gap-1">{rightSlot}</div>
    </header>
  );
}
