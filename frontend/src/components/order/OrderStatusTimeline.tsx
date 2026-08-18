import { IconCheckCircle } from "@/components/ui/icons";
import { cn } from "@/utils/cn";
import type { OrderStatus } from "@/types/order.types";

const STEPS: { status: OrderStatus; label: string }[] = [
  { status: "ACCEPTED", label: "접수" },
  { status: "PREPARING", label: "제조 중" },
  { status: "READY", label: "픽업 준비" },
  { status: "PICKED_UP", label: "픽업 완료" },
];

const STEP_INDEX: Partial<Record<OrderStatus, number>> = {
  PENDING: -1,
  ACCEPTED: 0,
  PREPARING: 1,
  READY: 2,
  PICKED_UP: 3,
};

export interface OrderStatusTimelineProps {
  currentStatus: OrderStatus;
}

/** 주문 진행 단계 타임라인 (F-04) */
export function OrderStatusTimeline({ currentStatus }: OrderStatusTimelineProps) {
  if (currentStatus === "CANCELLED") {
    return <p className="text-center text-sm font-semibold text-danger">주문이 취소됐어요</p>;
  }

  const currentIndex = STEP_INDEX[currentStatus] ?? -1;

  return (
    <ol className="flex items-start" aria-label="주문 진행 상태">
      {STEPS.map((step, index) => {
        const isDone = index <= currentIndex;
        return (
          <li key={step.status} className="flex flex-1 flex-col items-center gap-1.5">
            <div className="flex w-full items-center">
              <span
                className={cn("h-0.5 flex-1", index === 0 ? "invisible" : index <= currentIndex ? "bg-accent" : "bg-border")}
                aria-hidden="true"
              />
              <span
                className={cn(
                  "flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold",
                  isDone ? "bg-accent text-accent-foreground" : "bg-surface-muted text-foreground/70",
                )}
              >
                {isDone ? <IconCheckCircle className="h-4 w-4" /> : index + 1}
              </span>
              <span
                className={cn(
                  "h-0.5 flex-1",
                  index === STEPS.length - 1 ? "invisible" : index < currentIndex ? "bg-accent" : "bg-border",
                )}
                aria-hidden="true"
              />
            </div>
            <span className={cn("text-xs font-medium", isDone ? "text-foreground" : "text-foreground/70")}>
              {step.label}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
