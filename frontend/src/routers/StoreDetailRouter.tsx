"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Header } from "@/components/ui/layout/Header";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { CartBadgeLink } from "@/components/cart/CartBadgeLink";
import { GeofenceWarningModal } from "@/components/store/GeofenceWarningModal";
import { useGeolocation } from "@/hooks/useGeolocation";
import { useGeofenceCheck } from "@/hooks/useGeofenceCheck";
import { storeApi } from "@/api/storeApi";
import { MenuRouter } from "@/routers/MenuRouter";
import type { StoreDetail } from "@/types/store.types";

export interface StoreDetailRouterProps {
  initialStore: StoreDetail;
}

/** 매장 상세 페이지 조립. 서버에서 SSR로 받은 initialStore로 즉시 렌더링하고 클라이언트에서 최신화한다 */
export function StoreDetailRouter({ initialStore }: StoreDetailRouterProps) {
  const router = useRouter();
  const { data: store } = useQuery({
    queryKey: ["stores", "detail", initialStore.id],
    queryFn: () => storeApi.detail(initialStore.id),
    initialData: initialStore,
  });

  const geolocation = useGeolocation();
  const geofence = useGeofenceCheck(geolocation.coords, store.location);
  const [warningDismissed, setWarningDismissed] = useState(false);

  const showWarning = geolocation.status === "success" && geofence.needsWarning && !warningDismissed;

  return (
    <div className="flex min-h-screen flex-col">
      <Header title={store.name} onBack={() => router.back()} rightSlot={<CartBadgeLink />} />
      <main id="main-content" className="flex-1 pb-4">
        <div className="p-4">
          <Card className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <h1 className="text-xl font-bold text-foreground">{store.name}</h1>
              <Badge variant={store.isOpen ? "success" : "neutral"}>
                {store.isOpen ? "영업 중" : "영업 종료"}
              </Badge>
            </div>
            <p className="text-sm text-foreground/60">{store.address}</p>
            <p className="text-sm text-foreground/60">{store.businessHours}</p>
            <div className="flex flex-wrap gap-1.5 pt-1">
              <Badge variant={store.waitingOrderCount > 3 ? "warning" : "neutral"}>
                대기 {store.waitingOrderCount}건
              </Badge>
              <Badge variant="accent">예상 조리 {store.estimatedPrepMinutes}분</Badge>
            </div>
          </Card>
        </div>

        <MenuRouter storeId={store.id} storeName={store.name} />
      </main>

      <GeofenceWarningModal
        open={showWarning}
        storeName={store.name}
        distanceKm={geofence.distanceKm}
        onConfirm={() => setWarningDismissed(true)}
        onCancel={() => router.back()}
      />
    </div>
  );
}
