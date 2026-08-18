"use client";

import { BottomSheet } from "@/components/ui/BottomSheet";
import { Button } from "@/components/ui/Button";
import { OptionSelector } from "@/components/menu/OptionSelector";
import { useMenuOptions } from "@/hooks/useMenuOptions";
import type { MenuItem, SelectedOption } from "@/types/menu.types";

export interface MenuOptionSheetConfirmPayload {
  item: MenuItem;
  selections: SelectedOption[];
  quantity: number;
  /** 옵션이 반영된 1개 단가 (수량 미반영) */
  unitPrice: number;
  totalPrice: number;
}

export interface MenuOptionSheetProps {
  item: MenuItem | null;
  open: boolean;
  onClose: () => void;
  /** 선택 완료 시 호출된다. Phase 5에서 cartStore.addItem(...) 호출로 연결될 지점 */
  onConfirm: (payload: MenuOptionSheetConfirmPayload) => void;
}

/** 퍼스널 옵션 선택 바텀시트 (F-02) */
export function MenuOptionSheet({ item, open, onClose, onConfirm }: MenuOptionSheetProps) {
  if (!item) return null;
  return <MenuOptionSheetContent key={item.id} item={item} open={open} onClose={onClose} onConfirm={onConfirm} />;
}

function MenuOptionSheetContent({
  item,
  open,
  onClose,
  onConfirm,
}: {
  item: MenuItem;
  open: boolean;
  onClose: () => void;
  onConfirm: (payload: MenuOptionSheetConfirmPayload) => void;
}) {
  const options = useMenuOptions(item);

  const handleConfirm = () => {
    if (!options.isValid) return;
    onConfirm({
      item,
      selections: options.selections,
      quantity: options.quantity,
      unitPrice: options.unitPrice,
      totalPrice: options.totalPrice,
    });
    options.reset();
    onClose();
  };

  return (
    <BottomSheet
      open={open}
      onClose={onClose}
      title={item.name}
      footer={
        <Button type="button" fullWidth size="lg" disabled={!options.isValid} onClick={handleConfirm}>
          {options.totalPrice.toLocaleString()}원 담기
        </Button>
      }
    >
      <div className="space-y-5 pb-4">
        <p className="text-sm text-foreground/60">{item.description}</p>

        {item.optionGroups.map((group) => (
          <OptionSelector
            key={group.id}
            group={group}
            isSelected={(choiceId) => options.isChoiceSelected(group.id, choiceId)}
            onToggle={(choiceId) => options.toggleChoice(group, choiceId)}
            hasError={options.missingRequiredGroupIds.includes(group.id)}
          />
        ))}

        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold text-foreground">수량</span>
          <div className="flex items-center gap-3">
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => options.setQuantity(Math.max(1, options.quantity - 1))}
              aria-label="수량 감소"
            >
              -
            </Button>
            <span className="w-6 text-center font-semibold text-foreground">{options.quantity}</span>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => options.setQuantity(options.quantity + 1)}
              aria-label="수량 증가"
            >
              +
            </Button>
          </div>
        </div>
      </div>
    </BottomSheet>
  );
}
