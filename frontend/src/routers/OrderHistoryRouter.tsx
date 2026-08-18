"use client";

import { useQuery } from "@tanstack/react-query";
import { Header } from "@/components/ui/layout/Header";
import { BottomNav } from "@/components/ui/layout/BottomNav";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorAlert } from "@/components/ui/ErrorAlert";
import { EmptyState } from "@/components/ui/EmptyState";
import { IconReceipt } from "@/components/ui/icons";
import { OrderHistoryListItem } from "@/components/order/OrderHistoryListItem";
import { orderApi } from "@/api/orderApi";

/** 주문 내역 페이지 조립. 진행 중인 주문은 실시간 추적(F-04) 페이지로 이어진다 */
export function OrderHistoryRouter() {
  const historyQuery = useQuery({ queryKey: ["orders", "history"], queryFn: () => orderApi.history() });

  return (
    <div className="flex min-h-screen flex-col">
      <Header title="주문내역" />
      <main id="main-content" className="flex-1 pb-20">
        {historyQuery.isPending ? (
          <div className="space-y-3 p-4">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        ) : historyQuery.isError ? (
          <ErrorAlert layout="page" onRetry={() => historyQuery.refetch()} />
        ) : historyQuery.data.length === 0 ? (
          <EmptyState
            icon={<IconReceipt className="h-10 w-10" />}
            title="주문 내역이 없어요"
            description="첫 주문을 시작해보세요."
          />
        ) : (
          <div className="space-y-3 p-4">
            {historyQuery.data.map((order) => (
              <OrderHistoryListItem key={order.orderId} order={order} />
            ))}
          </div>
        )}
      </main>
      <BottomNav />
    </div>
  );
}
