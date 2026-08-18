"use client";

import { Spinner } from "@/components/ui/Spinner";
import { Button } from "@/components/ui/Button";
import { IconCheckCircle } from "@/components/ui/icons";

export interface CheckoutOverlayProps {
  status: "processing" | "success";
  orderId?: string | null;
  totalAmount?: number | null;
  onConfirm?: () => void;
}

/** 결제 처리 중/완료 전체화면 오버레이 (F-03) */
export function CheckoutOverlay({ status, orderId, totalAmount, onConfirm }: CheckoutOverlayProps) {
  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-background px-6 text-center">
      {status === "processing" ? (
        <>
          <Spinner className="h-10 w-10" label="결제 처리 중" />
          <p className="text-base font-semibold text-foreground">결제를 진행하고 있어요</p>
          <p className="text-sm text-foreground/60">잠시만 기다려주세요. 화면을 벗어나지 마세요.</p>
        </>
      ) : (
        <>
          <IconCheckCircle className="h-12 w-12 text-success" />
          <p className="text-lg font-bold text-foreground">주문이 완료됐어요</p>
          {orderId && <p className="text-sm text-foreground/60">주문번호 {orderId}</p>}
          {typeof totalAmount === "number" && (
            <p className="text-base font-semibold text-foreground">{totalAmount.toLocaleString()}원 결제 완료</p>
          )}
          <Button type="button" size="lg" fullWidth className="mt-2 max-w-xs" onClick={onConfirm}>
            확인
          </Button>
        </>
      )}
    </div>
  );
}
