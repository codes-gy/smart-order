"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { Header } from "@/components/ui/layout/Header";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Skeleton, SkeletonText } from "@/components/ui/Skeleton";
import { ConnectionStatusBadge } from "@/components/order/ConnectionStatusBadge";
import { OrderStatusTimeline } from "@/components/order/OrderStatusTimeline";
import { useOrderTracking } from "@/hooks/useOrderTracking";
import { usePushNotificationPermission } from "@/hooks/usePushNotificationPermission";
import { useToast } from "@/hooks/useToast";
import { vibrate } from "@/utils/vibration";

export interface OrderTrackingRouterProps {
  orderId: string;
}

/** 주문 실시간 추적(F-04) 페이지 조립: SSE(+backoff/polling 폴백) 상태를 화면에 반영한다 */
export function OrderTrackingRouter({ orderId }: OrderTrackingRouterProps) {
  const router = useRouter();
  const tracking = useOrderTracking(orderId);
  const push = usePushNotificationPermission();
  const { show } = useToast();
  const lastNotifiedStatusRef = useRef<string | null>(null);

  useEffect(() => {
    const event = tracking.latestEvent;
    if (!event || event.status === lastNotifiedStatusRef.current) return;
    lastNotifiedStatusRef.current = event.status;

    vibrate([80, 40, 80]);
    show({ title: event.message, variant: event.status === "CANCELLED" ? "error" : "success" });

    if (push.permission === "granted" && typeof document !== "undefined" && document.hidden) {
      new Notification("스마트오더", { body: event.message });
    }
  }, [tracking.latestEvent, push.permission, show]);

  return (
    <div className="flex min-h-screen flex-col">
      <Header title="주문 현황" onBack={() => router.back()} />
      <main id="main-content" className="flex-1 space-y-4 p-4">
        <div className="flex items-center justify-between">
          <p className="text-sm text-foreground/60">주문번호 {orderId}</p>
          <ConnectionStatusBadge status={tracking.connectionStatus} />
        </div>

        {!tracking.latestEvent ? (
          <Card className="space-y-4">
            <Skeleton className="h-16 w-full" />
            <SkeletonText lines={1} />
          </Card>
        ) : (
          <Card className="space-y-4">
            <OrderStatusTimeline currentStatus={tracking.latestEvent.status} />
            <p className="text-center text-sm text-foreground/70">{tracking.latestEvent.message}</p>
          </Card>
        )}

        {push.isSupported && push.permission !== "granted" && (
          <Card className="flex items-center justify-between gap-3">
            <p className="text-sm text-foreground/70">픽업 준비 완료 알림을 받아보세요</p>
            <Button type="button" size="sm" onClick={push.requestPermission}>
              알림 받기
            </Button>
          </Card>
        )}

        <Button type="button" variant="outline" fullWidth onClick={() => router.push("/orders/history")}>
          주문 내역으로
        </Button>
      </main>
    </div>
  );
}
