import { Badge, type BadgeVariant } from "@/components/ui/Badge";
import type { ConnectionStatus } from "@/types/common.types";

const LABEL: Record<ConnectionStatus, string> = {
  connecting: "연결 중",
  open: "실시간 연결됨",
  reconnecting: "재연결 중",
  polling: "주기적 갱신 중",
  closed: "연결 종료",
};

const VARIANT: Record<ConnectionStatus, BadgeVariant> = {
  connecting: "neutral",
  open: "success",
  reconnecting: "warning",
  polling: "warning",
  closed: "neutral",
};

export interface ConnectionStatusBadgeProps {
  status: ConnectionStatus;
}

/** SSE/Polling 연결 상태 표시 배지 (F-04) */
export function ConnectionStatusBadge({ status }: ConnectionStatusBadgeProps) {
  return <Badge variant={VARIANT[status]}>{LABEL[status]}</Badge>;
}
