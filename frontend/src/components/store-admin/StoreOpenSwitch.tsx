"use client";

import { Switch } from "@/components/ui/Switch";
import { useStoreOpenToggle } from "@/hooks/useStoreOpenToggle";

export interface StoreOpenSwitchProps {
  storeId: string;
}

/** 매장 "주문 받기 / 일시정지" 토글 (F-05) */
export function StoreOpenSwitch({ storeId }: StoreOpenSwitchProps) {
  const { isOpen, isPending, isTogglePending, setOpen } = useStoreOpenToggle(storeId);

  return (
    <div className="flex items-center gap-2">
      <span className="text-sm font-semibold text-foreground">
        {isPending ? "불러오는 중..." : isOpen ? "주문 받는 중" : "주문 일시정지"}
      </span>
      <Switch
        checked={isOpen}
        onCheckedChange={setOpen}
        disabled={isPending || isTogglePending}
        label="주문 받기 토글"
      />
    </div>
  );
}
