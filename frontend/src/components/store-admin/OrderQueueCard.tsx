import { Card } from "@/components/ui/Card";
import { Badge, type BadgeVariant } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import type { OrderHistoryItem, OrderStatus } from "@/types/order.types";

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "신규 접수",
  ACCEPTED: "접수됨",
  PREPARING: "제조 중",
  READY: "픽업 대기",
  PICKED_UP: "픽업 완료",
  CANCELLED: "취소됨",
};

const STATUS_VARIANT: Record<OrderStatus, BadgeVariant> = {
  PENDING: "warning",
  ACCEPTED: "accent",
  PREPARING: "accent",
  READY: "success",
  PICKED_UP: "neutral",
  CANCELLED: "danger",
};

const NEXT_ACTION_LABEL: Partial<Record<OrderStatus, string>> = {
  PENDING: "주문 접수",
  ACCEPTED: "제조 시작",
  PREPARING: "픽업 준비 완료",
  READY: "픽업 완료 처리",
};

export interface OrderQueueCardProps {
  order: OrderHistoryItem;
  onAdvance: (orderId: string) => void;
  onCancel: (orderId: string) => void;
  isBusy?: boolean;
}

/** 매장 관리자 주문 큐 카드. 다음 단계 진행/거절 버튼을 제공한다 (F-05) */
export function OrderQueueCard({ order, onAdvance, onCancel, isBusy }: OrderQueueCardProps) {
  const nextActionLabel = NEXT_ACTION_LABEL[order.status];
  const canCancel = order.status === "PENDING" || order.status === "ACCEPTED";

  return (
    <Card className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <p className="font-semibold text-foreground">{order.itemsSummary}</p>
        <Badge variant={STATUS_VARIANT[order.status]}>{STATUS_LABEL[order.status]}</Badge>
      </div>
      <div className="flex items-center justify-between text-sm text-foreground/60">
        <span>
          {new Date(order.createdAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
        </span>
        <span className="font-semibold text-foreground">{order.totalAmount.toLocaleString()}원</span>
      </div>
      {(nextActionLabel || canCancel) && (
        <div className="flex gap-2">
          {canCancel && (
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="flex-1"
              disabled={isBusy}
              onClick={() => onCancel(order.orderId)}
            >
              거절
            </Button>
          )}
          {nextActionLabel && (
            <Button
              type="button"
              size="lg"
              className="flex-1"
              disabled={isBusy}
              onClick={() => onAdvance(order.orderId)}
            >
              {nextActionLabel}
            </Button>
          )}
        </div>
      )}
    </Card>
  );
}
