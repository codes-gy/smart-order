import { ApiError, type CursorResponse } from "@/types/common.types";
import { MOCK_API_DELAY_MS } from "@/utils/constants";
import { haversineDistanceMeters } from "@/utils/geo";
import type {
  QuickReorderItem,
  StoreDetail,
  StoreListParams,
  StoreLocation,
  StoreSummary,
} from "@/types/store.types";

function delay(ms: number = MOCK_API_DELAY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const PAGE_SIZE = 10;

const BRANCH_NAMES = [
  "역삼역점", "강남대로점", "선릉점", "삼성동점", "교대역점", "논현점", "신논현점",
  "양재점", "도곡점", "대치점", "한티역점", "매봉역점", "서초점", "방배점",
  "이수역점", "사당점", "잠실점", "송파점", "건대입구점", "성수점", "왕십리점",
  "동대문점", "을지로점", "종각점", "시청점", "광화문점", "연신내점", "불광점",
  "합정점", "홍대입구점", "신촌점", "이대점", "공덕점", "여의도점", "영등포점",
];

function buildAllStores(): StoreDetail[] {
  const baseLat = 37.5006;
  const baseLng = 127.0364;

  return BRANCH_NAMES.map((branch, index) => {
    const latOffset = ((index % 7) - 3) * 0.004;
    const lngOffset = (Math.floor(index / 7) - 2) * 0.004;

    return {
      id: `store-${index + 1}`,
      name: `스마트오더 ${branch}`,
      address: `서울특별시 강남구 ${branch} ${index + 1}길 ${index + 10}`,
      location: { lat: baseLat + latOffset, lng: baseLng + lngOffset },
      distanceMeters: 0,
      waitingOrderCount: index % 6,
      estimatedPrepMinutes: 3 + (index % 5) * 2,
      isOpen: index % 9 !== 0,
      businessHours: "매일 07:00 - 22:00",
      phoneNumber: "02-1234-5678",
      description: "출근길에 딱 맞는 스페셜티 원두 카페",
    };
  });
}

const ALL_STORES = buildAllStores();

/** 매장 관리자가 토글한 "주문 받기/일시정지" 상태. 기본값(index 기반)을 덮어쓴다 */
const openOverrides = new Map<string, boolean>();

function resolveIsOpen(store: StoreDetail): boolean {
  return openOverrides.get(store.id) ?? store.isOpen;
}

function encodeCursor(offset: number): string {
  return btoa(JSON.stringify({ offset }));
}

function decodeCursor(cursor: string): number {
  try {
    const parsed = JSON.parse(atob(cursor)) as { offset?: number };
    return typeof parsed.offset === "number" ? parsed.offset : 0;
  } catch {
    return 0;
  }
}

export async function mockFetchStores(params: StoreListParams): Promise<CursorResponse<StoreSummary>> {
  await delay();

  const origin: StoreLocation = { lat: params.lat, lng: params.lng };
  const offset = params.cursor ? decodeCursor(params.cursor) : 0;
  const limit = params.limit ?? PAGE_SIZE;

  const sorted = ALL_STORES.map((store) => ({
    ...store,
    isOpen: resolveIsOpen(store),
    distanceMeters: Math.round(haversineDistanceMeters(origin, store.location)),
  })).sort((a, b) => a.distanceMeters - b.distanceMeters);

  const page = sorted.slice(offset, offset + limit);
  const nextOffset = offset + limit;
  const hasNext = nextOffset < sorted.length;

  return {
    result: page,
    meta: { nextCursor: hasNext ? encodeCursor(nextOffset) : null, hasNext },
  };
}

export async function mockFetchStoreDetail(storeId: string): Promise<StoreDetail> {
  await delay();
  const store = ALL_STORES.find((candidate) => candidate.id === storeId);
  if (!store) {
    throw new ApiError(404, { code: "STORE_NOT_FOUND", message: "매장을 찾을 수 없어요." });
  }
  return { ...store, isOpen: resolveIsOpen(store) };
}

/** 매장 관리자 대시보드의 "주문 받기/일시정지" 토글 (F-05) */
export async function mockSetStoreOpen(storeId: string, isOpen: boolean): Promise<void> {
  await delay(150);
  openOverrides.set(storeId, isOpen);
}

export async function mockFetchQuickReorders(): Promise<QuickReorderItem[]> {
  await delay();
  return [
    {
      storeId: "store-1",
      storeName: "스마트오더 역삼역점",
      menuSummary: "아이스 아메리카노 외 1건",
      lastOrderedAt: new Date(Date.now() - 86_400_000).toISOString(),
    },
    {
      storeId: "store-3",
      storeName: "스마트오더 선릉점",
      menuSummary: "카페라떼",
      lastOrderedAt: new Date(Date.now() - 3 * 86_400_000).toISOString(),
    },
  ];
}
