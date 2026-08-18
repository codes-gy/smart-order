"use client";

import { useRef, type KeyboardEvent } from "react";
import { cn } from "@/utils/cn";

export interface StickyTabItem {
  id: string;
  label: string;
}

export interface StickyTabsProps {
  items: StickyTabItem[];
  value: string;
  onChange: (id: string) => void;
  className?: string;
  /** 상단 고정이 필요한 컨테이너에서 top 오프셋을 지정 (헤더 높이만큼) */
  stickyOffsetClassName?: string;
}

/** 메뉴 카테고리 등에서 쓰는 가로 스크롤 스티키 탭. 방향키로 탭 간 이동 가능 */
export function StickyTabs({
  items,
  value,
  onChange,
  className,
  stickyOffsetClassName = "top-0",
}: StickyTabsProps) {
  const tabRefs = useRef<Record<string, HTMLButtonElement | null>>({});

  const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
    event.preventDefault();
    const nextIndex =
      event.key === "ArrowRight"
        ? (index + 1) % items.length
        : (index - 1 + items.length) % items.length;
    const nextItem = items[nextIndex];
    if (!nextItem) return;
    onChange(nextItem.id);
    tabRefs.current[nextItem.id]?.focus();
  };

  return (
    <div
      role="tablist"
      aria-label="메뉴 카테고리"
      className={cn(
        "sticky z-20 flex gap-1 overflow-x-auto border-b border-border bg-background px-4 py-2",
        "scrollbar-none snap-x",
        stickyOffsetClassName,
        className,
      )}
    >
      {items.map((item, index) => {
        const selected = item.id === value;
        return (
          <button
            key={item.id}
            ref={(node) => {
              tabRefs.current[item.id] = node;
            }}
            role="tab"
            type="button"
            aria-selected={selected}
            tabIndex={selected ? 0 : -1}
            onClick={() => onChange(item.id)}
            onKeyDown={(event) => handleKeyDown(event, index)}
            className={cn(
              "h-11 shrink-0 snap-start rounded-full px-4 text-sm font-medium transition-colors",
              selected
                ? "bg-primary text-primary-foreground"
                : "bg-surface-muted text-foreground/70 hover:text-foreground",
            )}
          >
            {item.label}
          </button>
        );
      })}
    </div>
  );
}
