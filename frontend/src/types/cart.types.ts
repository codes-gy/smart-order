import type { ID } from "@/types/common.types";
import type { SelectedOption } from "@/types/menu.types";

export interface CartOptionSummary {
  groupId: ID;
  groupName: string;
  choiceLabels: string[];
}

export interface CartLineItem {
  /** 메뉴 + 옵션 조합으로 결정되는 장바구니 라인 식별자 (동일 조합은 수량만 합산) */
  lineId: string;
  storeId: ID;
  storeName: string;
  menuId: ID;
  menuName: string;
  /** 옵션이 반영된 1개 단가 */
  unitPrice: number;
  quantity: number;
  selections: SelectedOption[];
  optionSummary: CartOptionSummary[];
}

export type PackagingType = "TAKE_OUT" | "DINE_IN";
