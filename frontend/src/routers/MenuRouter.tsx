"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Input } from "@/components/ui/Input";
import { Skeleton, SkeletonText } from "@/components/ui/Skeleton";
import { ErrorAlert } from "@/components/ui/ErrorAlert";
import { EmptyState } from "@/components/ui/EmptyState";
import { CategoryTabs } from "@/components/menu/CategoryTabs";
import { MenuCard } from "@/components/menu/MenuCard";
import { MenuOptionSheet, type MenuOptionSheetConfirmPayload } from "@/components/menu/MenuOptionSheet";
import { menuApi } from "@/api/menuApi";
import { useMenuSearch } from "@/hooks/useMenuSearch";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { useToast } from "@/hooks/useToast";
import { useCart } from "@/hooks/useCart";
import type { MenuItem } from "@/types/menu.types";

export interface MenuRouterProps {
  storeId: string;
  storeName: string;
}

/** 매장 상세 페이지에 삽입되는 메뉴 탐색 & 옵션 선택 영역 (F-02) */
export function MenuRouter({ storeId, storeName }: MenuRouterProps) {
  const menuQuery = useQuery({
    queryKey: ["stores", storeId, "menu"],
    queryFn: () => menuApi.getStoreMenu(storeId),
  });

  const [categoryId, setCategoryId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedItem, setSelectedItem] = useState<MenuItem | null>(null);
  const debouncedSearch = useDebouncedValue(searchTerm, 250);
  const { show } = useToast();
  const cart = useCart();

  const categories = menuQuery.data?.categories ?? [];
  const activeCategoryId = categoryId ?? categories[0]?.id ?? "";
  const filteredItems = useMenuSearch(menuQuery.data?.items ?? [], activeCategoryId, debouncedSearch);

  const handleConfirm = (payload: MenuOptionSheetConfirmPayload) => {
    const { switchedStore } = cart.addMenuSelection({
      storeId,
      storeName,
      item: payload.item,
      selections: payload.selections,
      quantity: payload.quantity,
      unitPrice: payload.unitPrice,
    });

    show({
      title: switchedStore ? "다른 매장으로 장바구니를 새로 시작했어요" : "장바구니에 담았어요",
      description: `${payload.item.name} ${payload.quantity}개 · ${payload.totalPrice.toLocaleString()}원`,
      variant: "success",
    });
  };

  if (menuQuery.isPending) {
    return (
      <div className="space-y-3 p-4">
        <Skeleton className="h-11 w-full" />
        <SkeletonText lines={2} />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
      </div>
    );
  }

  if (menuQuery.isError) {
    return <ErrorAlert layout="page" onRetry={() => menuQuery.refetch()} />;
  }

  if (categories.length === 0) {
    return <EmptyState title="등록된 메뉴가 없어요" />;
  }

  return (
    <div>
      <div className="px-4 pb-2 pt-3">
        <Input
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
          placeholder="메뉴 검색"
          aria-label="메뉴 검색"
        />
      </div>

      <CategoryTabs
        categories={categories}
        value={activeCategoryId}
        onChange={setCategoryId}
        stickyOffsetClassName="top-14"
      />

      <div className="space-y-3 p-4">
        {filteredItems.length === 0 ? (
          <EmptyState title="이 카테고리에 메뉴가 없어요" description="다른 카테고리나 검색어를 시도해보세요." />
        ) : (
          filteredItems.map((item) => <MenuCard key={item.id} item={item} onSelect={setSelectedItem} />)
        )}
      </div>

      <MenuOptionSheet
        item={selectedItem}
        open={selectedItem !== null}
        onClose={() => setSelectedItem(null)}
        onConfirm={handleConfirm}
      />
    </div>
  );
}
