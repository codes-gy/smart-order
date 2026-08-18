import type { ID } from "@/types/common.types";

export type MenuCategoryId = string;

export interface MenuCategory {
  id: MenuCategoryId;
  name: string;
}

export type OptionType = "single" | "multiple";

export interface MenuOptionChoice {
  id: ID;
  label: string;
  /** 기본가 대비 추가/할인 금액 (원). 할인은 음수 */
  priceDelta: number;
  isSoldOut: boolean;
}

export interface MenuOptionGroup {
  id: ID;
  /** 예: "온도", "컵 종류", "샷 추가", "시럽 추가", "우유 변경" */
  name: string;
  type: OptionType;
  required: boolean;
  choices: MenuOptionChoice[];
}

export interface MenuItem {
  id: ID;
  categoryId: MenuCategoryId;
  name: string;
  description: string;
  basePrice: number;
  isSoldOut: boolean;
  optionGroups: MenuOptionGroup[];
}

export interface StoreMenuResponse {
  categories: MenuCategory[];
  items: MenuItem[];
}

export interface SelectedOption {
  groupId: ID;
  choiceIds: ID[];
}
