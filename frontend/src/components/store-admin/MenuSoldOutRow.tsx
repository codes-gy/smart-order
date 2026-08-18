import { Switch } from "@/components/ui/Switch";
import type { MenuItem } from "@/types/menu.types";

export interface MenuSoldOutRowProps {
  item: MenuItem;
  onToggle: (menuId: string, isSoldOut: boolean) => void;
  disabled?: boolean;
}

/** 매장 관리자 메뉴 품절 토글 행 (F-05) */
export function MenuSoldOutRow({ item, onToggle, disabled }: MenuSoldOutRowProps) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-surface p-4">
      <div className="min-w-0">
        <p className="truncate font-semibold text-foreground">{item.name}</p>
        <p className="text-sm text-foreground/60">{item.basePrice.toLocaleString()}원</p>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-sm text-foreground/60">{item.isSoldOut ? "품절" : "판매 중"}</span>
        <Switch
          checked={!item.isSoldOut}
          onCheckedChange={(checked) => onToggle(item.id, !checked)}
          disabled={disabled}
          label={`${item.name} 판매 상태`}
        />
      </div>
    </div>
  );
}
