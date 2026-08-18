"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { storeApi } from "@/api/storeApi";
import type { StoreLocation } from "@/types/store.types";

const PAGE_LIMIT = 10;

/** PRD 4.3 커서 페이징 규격을 따르는 매장 리스트 무한 스크롤 훅 (F-01) */
export function useInfiniteStores(coords: StoreLocation) {
  return useInfiniteQuery({
    queryKey: ["stores", "list", coords.lat, coords.lng],
    queryFn: ({ pageParam }) =>
      storeApi.list({
        lat: coords.lat,
        lng: coords.lng,
        cursor: pageParam,
        limit: PAGE_LIMIT,
      }),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => (lastPage.meta.hasNext ? lastPage.meta.nextCursor : undefined),
  });
}
