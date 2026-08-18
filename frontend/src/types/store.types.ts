import type { ID } from "@/types/common.types";

export interface StoreLocation {
  lat: number;
  lng: number;
}

export interface StoreSummary {
  id: ID;
  name: string;
  address: string;
  location: StoreLocation;
  /** 요청 시점의 사용자 좌표 기준으로 서버가 계산해 내려주는 거리(m) */
  distanceMeters: number;
  waitingOrderCount: number;
  estimatedPrepMinutes: number;
  isOpen: boolean;
}

export interface StoreDetail extends StoreSummary {
  businessHours: string;
  phoneNumber: string;
  description: string;
}

export interface QuickReorderItem {
  storeId: ID;
  storeName: string;
  /** 예: "아이스 아메리카노 외 1건" */
  menuSummary: string;
  lastOrderedAt: string;
}

export interface StoreListParams {
  lat: number;
  lng: number;
  cursor?: string | null;
  limit?: number;
}
