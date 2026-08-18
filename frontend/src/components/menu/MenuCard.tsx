import { SoldOutBadge } from "@/components/menu/SoldOutBadge";
import { cn } from "@/utils/cn";
import type { MenuItem } from "@/types/menu.types";

export interface MenuCardProps {
  item: MenuItem;
  onSelect: (item: MenuItem) => void;
}

/** 메뉴 리스트 카드. 품절 시 비활성화된다 (F-02) */
export function MenuCard({ item, onSelect }: MenuCardProps) {
  return (
    <button
      type="button"
      disabled={item.isSoldOut}
      onClick={() => onSelect(item)}
      className={cn(
        "flex w-full items-start justify-between gap-3 rounded-2xl border border-border bg-surface p-4 text-left transition-colors",
        item.isSoldOut ? "opacity-50" : "hover:border-accent",
      )}
    >
      <div className="min-w-0 flex-1 space-y-1">
        <div className="flex items-center gap-2">
          <p className="font-semibold text-foreground">{item.name}</p>
          {item.isSoldOut && <SoldOutBadge />}
        </div>
        <p className="line-clamp-2 text-sm text-foreground/60">{item.description}</p>
        <p className="text-sm font-semibold text-foreground">{item.basePrice.toLocaleString()}원</p>
      </div>
    </button>
  );
}
