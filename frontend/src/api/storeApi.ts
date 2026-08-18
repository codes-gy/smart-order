import {
  mockFetchQuickReorders,
  mockFetchStoreDetail,
  mockFetchStores,
  mockSetStoreOpen,
} from "@/api/mock/storeMock";
import type { CursorResponse } from "@/types/common.types";
import type { QuickReorderItem, StoreDetail, StoreListParams, StoreSummary } from "@/types/store.types";

/** 매장 도메인 API 파사드. 현재는 Mock 구현에 위임한다 (PRD 6.B) */
export const storeApi = {
  list: (params: StoreListParams): Promise<CursorResponse<StoreSummary>> => mockFetchStores(params),
  detail: (storeId: string): Promise<StoreDetail> => mockFetchStoreDetail(storeId),
  quickReorders: (): Promise<QuickReorderItem[]> => mockFetchQuickReorders(),
  setOpen: (storeId: string, isOpen: boolean): Promise<void> => mockSetStoreOpen(storeId, isOpen),
};
