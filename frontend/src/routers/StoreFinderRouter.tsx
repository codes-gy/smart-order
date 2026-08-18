"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Header } from "@/components/ui/layout/Header";
import { BottomNav } from "@/components/ui/layout/BottomNav";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { CartBadgeLink } from "@/components/cart/CartBadgeLink";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Skeleton, SkeletonText } from "@/components/ui/Skeleton";
import { ErrorAlert } from "@/components/ui/ErrorAlert";
import { EmptyState } from "@/components/ui/EmptyState";
import { StoreSearchBar } from "@/components/store/StoreSearchBar";
import { StoreListItem } from "@/components/store/StoreListItem";
import { StoreMapView } from "@/components/store/StoreMapView";
import { QuickReorderCard } from "@/components/store/QuickReorderCard";
import { useGeolocation } from "@/hooks/useGeolocation";
import { useInfiniteStores } from "@/hooks/useInfiniteStores";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { storeApi } from "@/api/storeApi";

type ViewMode = "list" | "map";

/** 고객 홈: 매장 탐색(F-01) 페이지 조립 */
export function StoreFinderRouter() {
  const geolocation = useGeolocation();
  const [viewMode, setViewMode] = useState<ViewMode>("list");
  const [searchTerm, setSearchTerm] = useState("");
  const debouncedSearch = useDebouncedValue(searchTerm, 300);

  const storesQuery = useInfiniteStores(geolocation.coords);

  const quickReordersQuery = useQuery({
    queryKey: ["stores", "quick-reorders"],
    queryFn: () => storeApi.quickReorders(),
  });

  const allStores = useMemo(
    () => storesQuery.data?.pages.flatMap((page) => page.result) ?? [],
    [storesQuery.data],
  );

  const filteredStores = useMemo(() => {
    const keyword = debouncedSearch.trim();
    return keyword ? allStores.filter((store) => store.name.includes(keyword)) : allStores;
  }, [allStores, debouncedSearch]);

  const sentinelRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver((entries) => {
      const [entry] = entries;
      if (entry?.isIntersecting && storesQuery.hasNextPage && !storesQuery.isFetchingNextPage) {
        storesQuery.fetchNextPage();
      }
    });
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [storesQuery]);

  return (
    <div className="flex min-h-screen flex-col">
      <Header
        title="매장 찾기"
        rightSlot={
          <div className="flex items-center gap-0.5">
            <CartBadgeLink />
            <ThemeToggle />
          </div>
        }
      />
      <StoreSearchBar value={searchTerm} onChange={setSearchTerm} />

      {quickReordersQuery.data && quickReordersQuery.data.length > 0 && (
        <div className="flex gap-3 overflow-x-auto px-4 pb-2">
          {quickReordersQuery.data.map((item) => (
            <QuickReorderCard key={item.storeId} item={item} />
          ))}
        </div>
      )}

      <div className="flex justify-center gap-2 px-4 pb-2">
        <Button size="sm" variant={viewMode === "list" ? "primary" : "outline"} onClick={() => setViewMode("list")}>
          리스트
        </Button>
        <Button size="sm" variant={viewMode === "map" ? "primary" : "outline"} onClick={() => setViewMode("map")}>
          지도
        </Button>
      </div>

      <main id="main-content" className="flex-1 pb-20">
        {geolocation.status === "error" && geolocation.errorMessage && (
          <div className="mx-4 mb-2 flex items-center justify-between gap-2 rounded-xl bg-warning/10 px-3 py-2 text-sm text-warning-text">
            <span>{geolocation.errorMessage}</span>
            <button type="button" onClick={geolocation.retry} className="shrink-0 font-semibold underline">
              다시 시도
            </button>
          </div>
        )}

        {storesQuery.isPending ? (
          <div className="space-y-3 p-4">
            <SkeletonText lines={2} />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        ) : storesQuery.isError ? (
          <ErrorAlert layout="page" onRetry={() => storesQuery.refetch()} />
        ) : filteredStores.length === 0 ? (
          <EmptyState
            title="근처에 매장이 없어요"
            description="검색어를 변경하거나 다른 지역에서 시도해보세요."
          />
        ) : viewMode === "map" ? (
          <StoreMapView stores={filteredStores} userCoords={geolocation.coords} />
        ) : (
          <div className="space-y-3 p-4">
            {filteredStores.map((store) => (
              <StoreListItem key={store.id} store={store} />
            ))}
            <div ref={sentinelRef} className="h-4" aria-hidden="true" />
            {storesQuery.isFetchingNextPage && (
              <div className="flex justify-center py-4">
                <Spinner label="더 불러오는 중" />
              </div>
            )}
          </div>
        )}
      </main>

      <BottomNav />
    </div>
  );
}
