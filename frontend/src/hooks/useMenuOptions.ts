"use client";

import { useMemo, useState } from "react";
import type { MenuItem, MenuOptionGroup, SelectedOption } from "@/types/menu.types";

export interface UseMenuOptionsResult {
  quantity: number;
  setQuantity: (quantity: number) => void;
  selections: SelectedOption[];
  toggleChoice: (group: MenuOptionGroup, choiceId: string) => void;
  isChoiceSelected: (groupId: string, choiceId: string) => boolean;
  /** 옵션이 반영된 1개 단가 (수량 미반영) */
  unitPrice: number;
  /** unitPrice * quantity */
  totalPrice: number;
  missingRequiredGroupIds: string[];
  isValid: boolean;
  reset: () => void;
}

function buildInitialSelections(item: MenuItem): SelectedOption[] {
  return item.optionGroups.map((group) => {
    if (group.required && group.type === "single") {
      const firstAvailable = group.choices.find((choice) => !choice.isSoldOut);
      return { groupId: group.id, choiceIds: firstAvailable ? [firstAvailable.id] : [] };
    }
    return { groupId: group.id, choiceIds: [] };
  });
}

/** 메뉴 옵션 바텀시트(F-02)의 선택 상태와 실시간 금액 계산을 담당하는 훅 */
export function useMenuOptions(item: MenuItem): UseMenuOptionsResult {
  const [quantity, setQuantity] = useState(1);
  const [selections, setSelections] = useState<SelectedOption[]>(() => buildInitialSelections(item));

  const toggleChoice = (group: MenuOptionGroup, choiceId: string) => {
    setSelections((prev) =>
      prev.map((selection) => {
        if (selection.groupId !== group.id) return selection;

        if (group.type === "single") {
          return { ...selection, choiceIds: [choiceId] };
        }

        const alreadySelected = selection.choiceIds.includes(choiceId);
        return {
          ...selection,
          choiceIds: alreadySelected
            ? selection.choiceIds.filter((id) => id !== choiceId)
            : [...selection.choiceIds, choiceId],
        };
      }),
    );
  };

  const isChoiceSelected = (groupId: string, choiceId: string): boolean =>
    selections.find((selection) => selection.groupId === groupId)?.choiceIds.includes(choiceId) ?? false;

  const unitPrice = useMemo(() => {
    const optionsPrice = item.optionGroups.reduce((sum, group) => {
      const selection = selections.find((candidate) => candidate.groupId === group.id);
      if (!selection) return sum;
      const groupPrice = group.choices
        .filter((choice) => selection.choiceIds.includes(choice.id))
        .reduce((choiceSum, choice) => choiceSum + choice.priceDelta, 0);
      return sum + groupPrice;
    }, 0);
    return item.basePrice + optionsPrice;
  }, [item, selections]);

  const totalPrice = unitPrice * quantity;

  const missingRequiredGroupIds = useMemo(
    () =>
      item.optionGroups
        .filter((group) => group.required)
        .filter((group) => {
          const selection = selections.find((candidate) => candidate.groupId === group.id);
          return !selection || selection.choiceIds.length === 0;
        })
        .map((group) => group.id),
    [item, selections],
  );

  const reset = () => {
    setQuantity(1);
    setSelections(buildInitialSelections(item));
  };

  return {
    quantity,
    setQuantity,
    selections,
    toggleChoice,
    isChoiceSelected,
    unitPrice,
    totalPrice,
    missingRequiredGroupIds,
    isValid: missingRequiredGroupIds.length === 0,
    reset,
  };
}
