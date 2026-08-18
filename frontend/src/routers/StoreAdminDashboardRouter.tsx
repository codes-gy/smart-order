"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Header } from "@/components/ui/layout/Header";
import { StickyTabs, type StickyTabItem } from "@/components/ui/StickyTabs";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorAlert } from "@/components/ui/ErrorAlert";
import { EmptyState } from "@/components/ui/EmptyState";
import { IconReceipt } from "@/components/ui/icons";
import { OrderQueueCard } from "@/components/store-admin/OrderQueueCard";
import { MenuSoldOutRow } from "@/components/store-admin/MenuSoldOutRow";
import { StoreOpenSwitch } from "@/components/store-admin/StoreOpenSwitch";
import { useStoreAdminOrders } from "@/hooks/useStoreAdminOrders";
import { useStoreAdminMenu } from "@/hooks/useStoreAdminMenu";
import { useAuthStore } from "@/stores/authStore";
import type { OrderStatus } from "@/types/order.types";

type DashboardTab = "orders" | "menu";

const TABS: StickyTabItem[] = [
  { id: "orders", label: "주문" },
  { id: "menu", label: "메뉴" },
];

const ACTIVE_STATUSES: OrderStatus[] = ["PENDING", "ACCEPTED", "PREPARING", "READY"];

/** 매장 관리자 대시보드(F-05) 페이지 조립: 실시간 주문 큐 처리 + 메뉴 품절 토글 */
export function StoreAdminDashboardRouter() {
  const router = useRouter();
  const storeSession = useAuthStore((state) => state.storeSession);
  const clearStoreSession = useAuthStore((state) => state.clearStoreSession);
  const [tab, setTab] = useState<DashboardTab>("orders");

  const storeId = storeSession?.storeId ?? "store-1";
  const orders = useStoreAdminOrders(storeId);
  const menu = useStoreAdminMenu(storeId);

  const activeOrders = (orders.ordersQuery.data ?? []).filter((order) =>
    ACTIVE_STATUSES.includes(order.status),
  );

  const handleLogout = () => {
    clearStoreSession();
    router.push("/store-admin/login");
  };

  return (
    <div className="flex min-h-screen flex-col">
      <Header
        title={storeSession?.storeName ?? "매장 관리자"}
        rightSlot={
          <Button type="button" variant="ghost" size="sm" onClick={handleLogout}>
            로그아웃
          </Button>
        }
      />

      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <StoreOpenSwitch storeId={storeId} />
      </div>

      <StickyTabs
        items={TABS}
        value={tab}
        onChange={(id) => setTab(id as DashboardTab)}
        stickyOffsetClassName="top-14"
      />

      <main id="main-content" className="flex-1 space-y-3 p-4">
        {tab === "orders" ? (
          orders.ordersQuery.isPending ? (
            <div className="space-y-3">
              <Skeleton className="h-28 w-full" />
              <Skeleton className="h-28 w-full" />
            </div>
          ) : orders.ordersQuery.isError ? (
            <ErrorAlert layout="page" onRetry={() => orders.ordersQuery.refetch()} />
          ) : activeOrders.length === 0 ? (
            <EmptyState
              icon={<IconReceipt className="h-10 w-10" />}
              title="진행 중인 주문이 없어요"
              description="새 주문이 들어오면 여기에 표시돼요."
            />
          ) : (
            activeOrders.map((order) => (
              <OrderQueueCard
                key={order.orderId}
                order={order}
                onAdvance={orders.advanceOrder}
                onCancel={orders.cancelOrder}
                isBusy={orders.isMutating}
              />
            ))
          )
        ) : menu.menuQuery.isPending ? (
          <div className="space-y-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : menu.menuQuery.isError ? (
          <ErrorAlert layout="page" onRetry={() => menu.menuQuery.refetch()} />
        ) : (menu.menuQuery.data?.items.length ?? 0) === 0 ? (
          <EmptyState title="등록된 메뉴가 없어요" />
        ) : (
          menu.menuQuery.data?.items.map((item) => (
            <MenuSoldOutRow key={item.id} item={item} onToggle={menu.toggleSoldOut} disabled={menu.isToggling} />
          ))
        )}
      </main>
    </div>
  );
}
