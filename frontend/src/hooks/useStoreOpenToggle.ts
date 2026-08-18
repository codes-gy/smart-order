"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { storeApi } from "@/api/storeApi";
import type { StoreDetail } from "@/types/store.types";

export interface UseStoreOpenToggleResult {
  isOpen: boolean;
  isPending: boolean;
  isTogglePending: boolean;
  setOpen: (isOpen: boolean) => void;
}

/** 매장 관리자 대시보드(F-05)의 "주문 받기/일시정지" 토글 */
export function useStoreOpenToggle(storeId: string): UseStoreOpenToggleResult {
  const queryClient = useQueryClient();
  const queryKey = ["stores", "detail", storeId];

  const storeQuery = useQuery({
    queryKey,
    queryFn: () => storeApi.detail(storeId),
    enabled: Boolean(storeId),
  });

  const toggleMutation = useMutation({
    mutationFn: (isOpen: boolean) => storeApi.setOpen(storeId, isOpen),
    onSuccess: (_data, isOpen) => {
      queryClient.setQueryData<StoreDetail>(queryKey, (prev) => (prev ? { ...prev, isOpen } : prev));
    },
  });

  return {
    isOpen: storeQuery.data?.isOpen ?? true,
    isPending: storeQuery.isPending,
    isTogglePending: toggleMutation.isPending,
    setOpen: (isOpen: boolean) => toggleMutation.mutate(isOpen),
  };
}
