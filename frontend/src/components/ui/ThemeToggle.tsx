"use client";

import { useTheme } from "@/hooks/useTheme";
import type { ThemeMode } from "@/stores/themeStore";
import { IconMonitor, IconMoon, IconSun } from "@/components/ui/icons";
import { cn } from "@/utils/cn";

const OPTIONS: { mode: ThemeMode; label: string; icon: React.ReactNode }[] = [
  { mode: "light", label: "라이트 모드", icon: <IconSun className="h-4 w-4" /> },
  { mode: "system", label: "시스템 설정", icon: <IconMonitor className="h-4 w-4" /> },
  { mode: "dark", label: "다크 모드", icon: <IconMoon className="h-4 w-4" /> },
];

/** 고객용 3단(시스템/라이트/다크) 테마 전환 컨트롤 */
export function ThemeToggle({ className }: { className?: string }) {
  const { mode, setMode } = useTheme();

  return (
    <div
      role="radiogroup"
      aria-label="테마 선택"
      className={cn("inline-flex items-center gap-1 rounded-full bg-surface-muted p-1", className)}
    >
      {OPTIONS.map((option) => {
        const selected = option.mode === mode;
        return (
          <button
            key={option.mode}
            type="button"
            role="radio"
            aria-checked={selected}
            aria-label={option.label}
            onClick={() => setMode(option.mode)}
            className={cn(
              "flex h-10 w-10 items-center justify-center rounded-full transition-colors",
              selected
                ? "bg-primary text-primary-foreground"
                : "text-foreground/60 hover:text-foreground",
            )}
          >
            {option.icon}
          </button>
        );
      })}
    </div>
  );
}
