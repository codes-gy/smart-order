"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { useIsClient } from "@/hooks/useIsClient";
import { cn } from "@/utils/cn";

export interface BottomSheetProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children?: ReactNode;
  footer?: ReactNode;
  className?: string;
}

/**
 * 모바일 바텀시트. 메뉴 퍼스널 옵션 선택(F-02) 등 모바일 특화 인터랙션에 사용한다.
 * 내부 콘텐츠 영역은 뷰포트 높이를 넘지 않도록 스크롤 처리한다.
 */
export function BottomSheet({ open, onClose, title, children, footer, className }: BottomSheetProps) {
  const mounted = useIsClient();
  const sheetRef = useRef<HTMLDivElement>(null);
  const titleId = useId();

  useEffect(() => {
    if (!open) return;

    sheetRef.current?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [open, onClose]);

  if (!mounted || !open) return null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} aria-hidden="true" />
      <div
        ref={sheetRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? titleId : undefined}
        tabIndex={-1}
        className={cn(
          "relative z-10 flex max-h-[85vh] w-full flex-col rounded-t-3xl bg-surface pb-[env(safe-area-inset-bottom)] shadow-lg outline-none sm:max-w-md sm:rounded-3xl",
          className,
        )}
      >
        <div className="flex justify-center pt-3">
          <span className="h-1.5 w-10 rounded-full bg-border" aria-hidden="true" />
        </div>
        {title && (
          <h2 id={titleId} className="px-5 pb-2 pt-3 text-lg font-semibold text-foreground">
            {title}
          </h2>
        )}
        <div className="flex-1 overflow-y-auto px-5">{children}</div>
        {footer && <div className="border-t border-border px-5 py-4">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}
