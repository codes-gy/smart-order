import Link from "next/link";
import { Card } from "@/components/ui/Card";
import { Badge, type BadgeVariant } from "@/components/ui/Badge";
import type { OrderHistoryItem, OrderStatus } from "@/types/order.types";

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "접수 중",
  ACCEPTED: "접수 완료",
  PREPARING: "제조 중",
  READY: "픽업 준비 완료",
  PICKED_UP: "픽업 완료",
  CANCELLED: "취소됨",
};

const STATUS_VARIANT: Record<OrderStatus, BadgeVariant> = {
  PENDING: "accent",
  ACCEPTED: "accent",
  PREPARING: "accent",
  READY: "accent",
  PICKED_UP: "neutral",
  CANCELLED: "danger",
};

const ACTIVE_STATUSES: OrderStatus[] = ["PENDING", "ACCEPTED", "PREPARING", "READY"];

export interface OrderHistoryListItemProps {
  order: OrderHistoryItem;
}

/** 주문 내역 리스트 아이템. 진행 중인 주문만 추적 페이지로 이동한다 (F-04) */
export function OrderHistoryListItem({ order }: OrderHistoryListItemProps) {
  const isActive = ACTIVE_STATUSES.includes(order.status);

  const body = (
    <Card className="space-y-1.5">
      <div className="flex items-center justify-between gap-2">
        <p className="truncate font-semibold text-foreground">{order.storeName}</p>
        <Badge variant={STATUS_VARIANT[order.status]}>{STATUS_LABEL[order.status]}</Badge>
      </div>
      <p className="truncate text-sm text-foreground/60">{order.itemsSummary}</p>
      <div className="flex items-center justify-between text-sm text-foreground/70">
        <span>{new Date(order.createdAt).toLocaleDateString("ko-KR")}</span>
        <span className="font-semibold text-foreground">{order.totalAmount.toLocaleString()}원</span>
      </div>
    </Card>
  );

  if (!isActive) return <div>{body}</div>;

  return (
    <Link href={`/orders/${order.orderId}/track`} className="block">
      {body}
    </Link>
  );
}
