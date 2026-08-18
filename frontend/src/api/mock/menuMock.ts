import { MOCK_API_DELAY_MS } from "@/utils/constants";
import type { MenuCategory, MenuItem, MenuOptionGroup, StoreMenuResponse } from "@/types/menu.types";

function delay(ms: number = MOCK_API_DELAY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function hashString(input: string): number {
  let hash = 0;
  for (let i = 0; i < input.length; i += 1) {
    hash = input.charCodeAt(i) + ((hash << 5) - hash);
  }
  return Math.abs(hash);
}

const CATEGORIES: MenuCategory[] = [
  { id: "coffee", name: "커피" },
  { id: "tea", name: "티" },
  { id: "smoothie", name: "스무디" },
  { id: "dessert", name: "디저트" },
];

const TEMPERATURE_OPTION: MenuOptionGroup = {
  id: "opt-temperature",
  name: "온도",
  type: "single",
  required: true,
  choices: [
    { id: "temp-ice", label: "ICE", priceDelta: 0, isSoldOut: false },
    { id: "temp-hot", label: "HOT", priceDelta: 0, isSoldOut: false },
  ],
};

const CUP_OPTION: MenuOptionGroup = {
  id: "opt-cup",
  name: "컵 종류",
  type: "single",
  required: true,
  choices: [
    { id: "cup-disposable", label: "일회용컵", priceDelta: 0, isSoldOut: false },
    { id: "cup-personal", label: "개인컵", priceDelta: -300, isSoldOut: false },
    { id: "cup-store", label: "매장컵", priceDelta: 0, isSoldOut: false },
  ],
};

const SHOT_OPTION: MenuOptionGroup = {
  id: "opt-shot",
  name: "샷 추가",
  type: "multiple",
  required: false,
  choices: [
    { id: "shot-extra", label: "샷 추가", priceDelta: 500, isSoldOut: false },
    { id: "shot-decaf", label: "디카페인 변경", priceDelta: 0, isSoldOut: false },
  ],
};

const SYRUP_OPTION: MenuOptionGroup = {
  id: "opt-syrup",
  name: "시럽 추가",
  type: "multiple",
  required: false,
  choices: [
    { id: "syrup-vanilla", label: "바닐라 시럽", priceDelta: 0, isSoldOut: false },
    { id: "syrup-hazelnut", label: "헤이즐넛 시럽", priceDelta: 0, isSoldOut: false },
  ],
};

const MILK_OPTION: MenuOptionGroup = {
  id: "opt-milk",
  name: "우유 변경",
  type: "single",
  required: false,
  choices: [
    { id: "milk-normal", label: "일반우유", priceDelta: 0, isSoldOut: false },
    { id: "milk-lowfat", label: "저지방우유", priceDelta: 0, isSoldOut: false },
    { id: "milk-oat", label: "오트밀크", priceDelta: 700, isSoldOut: true },
    { id: "milk-soy", label: "두유", priceDelta: 500, isSoldOut: false },
  ],
};

function buildBaseItems(): MenuItem[] {
  return [
    {
      id: "menu-americano",
      categoryId: "coffee",
      name: "아메리카노",
      description: "깔끔한 스페셜티 원두 아메리카노",
      basePrice: 4500,
      isSoldOut: false,
      optionGroups: [TEMPERATURE_OPTION, CUP_OPTION, SHOT_OPTION],
    },
    {
      id: "menu-latte",
      categoryId: "coffee",
      name: "카페라떼",
      description: "부드러운 우유 거품의 라떼",
      basePrice: 5000,
      isSoldOut: false,
      optionGroups: [TEMPERATURE_OPTION, CUP_OPTION, SHOT_OPTION, MILK_OPTION],
    },
    {
      id: "menu-vanilla-latte",
      categoryId: "coffee",
      name: "바닐라라떼",
      description: "달콤한 바닐라 시럽이 더해진 라떼",
      basePrice: 5500,
      isSoldOut: false,
      optionGroups: [TEMPERATURE_OPTION, CUP_OPTION, SHOT_OPTION, MILK_OPTION],
    },
    {
      id: "menu-cold-brew",
      categoryId: "coffee",
      name: "콜드브루",
      description: "12시간 저온 추출한 콜드브루",
      basePrice: 5000,
      isSoldOut: true,
      optionGroups: [CUP_OPTION],
    },
    {
      id: "menu-earl-grey",
      categoryId: "tea",
      name: "얼그레이 티",
      description: "은은한 베르가못 향의 홍차",
      basePrice: 4800,
      isSoldOut: false,
      optionGroups: [TEMPERATURE_OPTION, CUP_OPTION, SYRUP_OPTION],
    },
    {
      id: "menu-peach-tea",
      categoryId: "tea",
      name: "복숭아 아이스티",
      description: "상큼한 복숭아 과육이 들어간 아이스티",
      basePrice: 5200,
      isSoldOut: false,
      optionGroups: [CUP_OPTION],
    },
    {
      id: "menu-green-smoothie",
      categoryId: "smoothie",
      name: "그린 스무디",
      description: "케일과 사과가 들어간 건강 스무디",
      basePrice: 6000,
      isSoldOut: false,
      optionGroups: [CUP_OPTION],
    },
    {
      id: "menu-mango-smoothie",
      categoryId: "smoothie",
      name: "망고 스무디",
      description: "달콤한 제철 망고 스무디",
      basePrice: 6200,
      isSoldOut: false,
      optionGroups: [CUP_OPTION],
    },
    {
      id: "menu-croissant",
      categoryId: "dessert",
      name: "버터 크루아상",
      description: "겹겹이 바삭한 프랑스식 크루아상",
      basePrice: 4200,
      isSoldOut: false,
      optionGroups: [],
    },
    {
      id: "menu-scone",
      categoryId: "dessert",
      name: "플레인 스콘",
      description: "담백한 오리지널 스콘",
      basePrice: 3800,
      isSoldOut: false,
      optionGroups: [],
    },
  ];
}

const BASE_ITEMS = buildBaseItems();

/** 매장 관리자가 토글한 품절 상태. storeId -> menuId -> isSoldOut */
const soldOutOverrides = new Map<string, Map<string, boolean>>();

/** storeId별로 결정적인(deterministic) 품절 변주를 줘 매장마다 재고 상황이 다르게 보이도록 한다 */
export async function mockFetchStoreMenu(storeId: string): Promise<StoreMenuResponse> {
  await delay();
  const seed = hashString(storeId);
  const overrides = soldOutOverrides.get(storeId);

  const items = BASE_ITEMS.map((item, index) => {
    const defaultSoldOut = item.isSoldOut || (seed + index) % 11 === 0;
    const isSoldOut = overrides?.get(item.id) ?? defaultSoldOut;
    return { ...item, isSoldOut };
  });

  return { categories: CATEGORIES, items };
}

/** 매장 관리자 대시보드의 메뉴 품절 토글 (F-05) */
export async function mockSetMenuItemSoldOut(storeId: string, menuId: string, isSoldOut: boolean): Promise<void> {
  await delay(150);
  const storeOverrides = soldOutOverrides.get(storeId) ?? new Map<string, boolean>();
  storeOverrides.set(menuId, isSoldOut);
  soldOutOverrides.set(storeId, storeOverrides);
}
