"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { IconHome, IconReceipt, IconUser } from "@/components/ui/icons";
import { cn } from "@/utils/cn";

export interface BottomNavItem {
  href: string;
  label: string;
  icon: ReactNode;
}

const DEFAULT_ITEMS: BottomNavItem[] = [
  { href: "/", label: "홈", icon: <IconHome className="h-5 w-5" /> },
  { href: "/orders/history", label: "주문내역", icon: <IconReceipt className="h-5 w-5" /> },
  { href: "/mypage", label: "마이페이지", icon: <IconUser className="h-5 w-5" /> },
];

export interface BottomNavProps {
  items?: BottomNavItem[];
  className?: string;
}

/** 고객용 모바일 하단 내비게이션. 각 탭은 48px 이상의 터치 영역을 갖는다 */
export function BottomNav({ items = DEFAULT_ITEMS, className }: BottomNavProps) {
  const pathname = usePathname();

  return (
    <nav
      aria-label="하단 내비게이션"
      className={cn(
        "sticky bottom-0 z-30 flex border-t border-border bg-background pb-[env(safe-area-inset-bottom)]",
        className,
      )}
    >
      {items.map((item) => {
        const isActive = pathname === item.href;
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={isActive ? "page" : undefined}
            className={cn(
              "flex h-14 flex-1 flex-col items-center justify-center gap-0.5 text-xs font-medium",
              isActive ? "text-primary" : "text-foreground/70",
            )}
          >
            {item.icon}
            <span>{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
