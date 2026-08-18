import { mockFetchStoreMenu, mockSetMenuItemSoldOut } from "@/api/mock/menuMock";
import type { StoreMenuResponse } from "@/types/menu.types";

/** 메뉴 도메인 API 파사드. 현재는 Mock 구현에 위임한다 (PRD 6.B) */
export const menuApi = {
  getStoreMenu: (storeId: string): Promise<StoreMenuResponse> => mockFetchStoreMenu(storeId),
  setSoldOut: (storeId: string, menuId: string, isSoldOut: boolean): Promise<void> =>
    mockSetMenuItemSoldOut(storeId, menuId, isSoldOut),
};
