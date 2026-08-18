"use client";

import { useMemo } from "react";
import { useCartStore } from "@/stores/cartStore";
import type { CartLineItem, CartOptionSummary, PackagingType } from "@/types/cart.types";
import type { MenuItem, SelectedOption } from "@/types/menu.types";

export interface AddMenuSelectionInput {
  storeId: string;
  storeName: string;
  item: MenuItem;
  selections: SelectedOption[];
  quantity: number;
  unitPrice: number;
}

function buildLineId(menuId: string, selections: SelectedOption[]): string {
  return [
    menuId,
    ...selections
      .map((selection) => `${selection.groupId}:${[...selection.choiceIds].sort().join(",")}`)
      .sort(),
  ].join("|");
}

function buildOptionSummary(item: MenuItem, selections: SelectedOption[]): CartOptionSummary[] {
  return item.optionGroups
    .map((group) => {
      const selection = selections.find((candidate) => candidate.groupId === group.id);
      const choiceLabels = (selection?.choiceIds ?? [])
        .map((choiceId) => group.choices.find((choice) => choice.id === choiceId)?.label)
        .filter((label): label is string => Boolean(label));
      return { groupId: group.id, groupName: group.name, choiceLabels };
    })
    .filter((summary) => summary.choiceLabels.length > 0);
}

export interface UseCartResult {
  items: CartLineItem[];
  itemCount: number;
  subtotal: number;
  packagingType: PackagingType;
  couponId: string | null;
  useStamp: boolean;
  storeId: string | null;
  storeName: string | null;
  /** 장바구니에 담기 전 다른 매장 내역이 있었는지 여부 (담기 직전 값을 반환) */
  addMenuSelection: (input: AddMenuSelectionInput) => { switchedStore: boolean };
  removeItem: (lineId: string) => void;
  setQuantity: (lineId: string, quantity: number) => void;
  setPackagingType: (packagingType: PackagingType) => void;
  setCouponId: (couponId: string | null) => void;
  setUseStamp: (useStamp: boolean) => void;
  clear: () => void;
}

/** 장바구니(F-03) 상태를 다루는 훅. cartStore(zustand persist)를 감싸 파생값을 계산한다 */
export function useCart(): UseCartResult {
  const items = useCartStore((state) => state.items);
  const packagingType = useCartStore((state) => state.packagingType);
  const couponId = useCartStore((state) => state.couponId);
  const useStamp = useCartStore((state) => state.useStamp);
  const addItem = useCartStore((state) => state.addItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const setQuantity = useCartStore((state) => state.setQuantity);
  const setPackagingType = useCartStore((state) => state.setPackagingType);
  const setCouponId = useCartStore((state) => state.setCouponId);
  const setUseStamp = useCartStore((state) => state.setUseStamp);
  const clear = useCartStore((state) => state.clear);

  const subtotal = useMemo(
    () => items.reduce((sum, line) => sum + line.unitPrice * line.quantity, 0),
    [items],
  );
  const itemCount = useMemo(() => items.reduce((sum, line) => sum + line.quantity, 0), [items]);

  const addMenuSelection = ({
    storeId,
    storeName,
    item,
    selections,
    quantity,
    unitPrice,
  }: AddMenuSelectionInput): { switchedStore: boolean } => {
    const switchedStore = items.length > 0 && items[0].storeId !== storeId;
    addItem({
      lineId: buildLineId(item.id, selections),
      storeId,
      storeName,
      menuId: item.id,
      menuName: item.name,
      unitPrice,
      quantity,
      selections,
      optionSummary: buildOptionSummary(item, selections),
    });
    return { switchedStore };
  };

  return {
    items,
    itemCount,
    subtotal,
    packagingType,
    couponId,
    useStamp,
    storeId: items[0]?.storeId ?? null,
    storeName: items[0]?.storeName ?? null,
    addMenuSelection,
    removeItem,
    setQuantity,
    setPackagingType,
    setCouponId,
    setUseStamp,
    clear,
  };
}
