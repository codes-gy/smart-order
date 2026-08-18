import { create } from "zustand";
import { persist } from "zustand/middleware";
import { CART_STORAGE_KEY } from "@/utils/constants";
import type { CartLineItem, PackagingType } from "@/types/cart.types";

interface CartState {
  items: CartLineItem[];
  packagingType: PackagingType;
  couponId: string | null;
  useStamp: boolean;
  addItem: (item: CartLineItem) => void;
  removeItem: (lineId: string) => void;
  setQuantity: (lineId: string, quantity: number) => void;
  setPackagingType: (packagingType: PackagingType) => void;
  setCouponId: (couponId: string | null) => void;
  setUseStamp: (useStamp: boolean) => void;
  clear: () => void;
}

/**
 * 장바구니(F-03) 상태. 한 번에 한 매장 주문만 지원하는 PRD 규칙에 따라,
 * 다른 매장의 메뉴를 담으면 기존 내역을 비우고 새로 시작한다.
 */
export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      packagingType: "TAKE_OUT",
      couponId: null,
      useStamp: false,

      addItem: (item) =>
        set((state) => {
          const isDifferentStore = state.items.length > 0 && state.items[0].storeId !== item.storeId;
          const baseItems = isDifferentStore ? [] : state.items;
          const existing = baseItems.find((line) => line.lineId === item.lineId);
          const items = existing
            ? baseItems.map((line) =>
                line.lineId === item.lineId
                  ? { ...line, quantity: line.quantity + item.quantity }
                  : line,
              )
            : [...baseItems, item];
          return {
            items,
            couponId: isDifferentStore ? null : state.couponId,
            useStamp: isDifferentStore ? false : state.useStamp,
          };
        }),

      removeItem: (lineId) =>
        set((state) => ({ items: state.items.filter((line) => line.lineId !== lineId) })),

      setQuantity: (lineId, quantity) =>
        set((state) => ({
          items:
            quantity <= 0
              ? state.items.filter((line) => line.lineId !== lineId)
              : state.items.map((line) => (line.lineId === lineId ? { ...line, quantity } : line)),
        })),

      setPackagingType: (packagingType) => set({ packagingType }),
      setCouponId: (couponId) => set({ couponId }),
      setUseStamp: (useStamp) => set({ useStamp }),
      clear: () => set({ items: [], couponId: null, useStamp: false }),
    }),
    {
      name: CART_STORAGE_KEY,
    },
  ),
);
