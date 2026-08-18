"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { menuApi } from "@/api/menuApi";
import type { StoreMenuResponse } from "@/types/menu.types";

export interface UseStoreAdminMenuResult {
  menuQuery: ReturnType<typeof useQuery<StoreMenuResponse>>;
  toggleSoldOut: (menuId: string, isSoldOut: boolean) => void;
  isToggling: boolean;
}

/** 매장 관리자 대시보드(F-05)의 메뉴 품절 토글. 성공 시 캐시를 낙관적으로 갱신한다 */
export function useStoreAdminMenu(storeId: string): UseStoreAdminMenuResult {
  const queryClient = useQueryClient();
  const queryKey = ["stores", storeId, "menu"];

  const menuQuery = useQuery({
    queryKey,
    queryFn: () => menuApi.getStoreMenu(storeId),
    enabled: Boolean(storeId),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ menuId, isSoldOut }: { menuId: string; isSoldOut: boolean }) =>
      menuApi.setSoldOut(storeId, menuId, isSoldOut),
    onSuccess: (_data, variables) => {
      queryClient.setQueryData<StoreMenuResponse>(queryKey, (prev) =>
        prev
          ? {
              ...prev,
              items: prev.items.map((item) =>
                item.id === variables.menuId ? { ...item, isSoldOut: variables.isSoldOut } : item,
              ),
            }
          : prev,
      );
    },
  });

  return {
    menuQuery,
    toggleSoldOut: (menuId: string, isSoldOut: boolean) => toggleMutation.mutate({ menuId, isSoldOut }),
    isToggling: toggleMutation.isPending,
  };
}
