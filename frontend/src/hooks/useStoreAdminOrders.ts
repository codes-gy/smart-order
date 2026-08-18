"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orderApi } from "@/api/orderApi";
import type { OrderHistoryItem } from "@/types/order.types";

export interface UseStoreAdminOrdersResult {
  ordersQuery: ReturnType<typeof useQuery<OrderHistoryItem[]>>;
  advanceOrder: (orderId: string) => void;
  cancelOrder: (orderId: string) => void;
  isMutating: boolean;
}

/** 매장 관리자 대시보드(F-05)의 주문 큐 조회 + 상태 진행/취소. 4초 주기로 다시 불러온다 */
export function useStoreAdminOrders(storeId: string): UseStoreAdminOrdersResult {
  const queryClient = useQueryClient();
  const queryKey = ["store-admin", "orders", storeId];

  const ordersQuery = useQuery({
    queryKey,
    queryFn: () => orderApi.storeQueue(storeId),
    enabled: Boolean(storeId),
    refetchInterval: 4000,
  });

  const advanceMutation = useMutation({
    mutationFn: (orderId: string) => orderApi.advance(orderId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  const cancelMutation = useMutation({
    mutationFn: (orderId: string) => orderApi.cancel(orderId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  return {
    ordersQuery,
    advanceOrder: (orderId: string) => advanceMutation.mutate(orderId),
    cancelOrder: (orderId: string) => cancelMutation.mutate(orderId),
    isMutating: advanceMutation.isPending || cancelMutation.isPending,
  };
}
