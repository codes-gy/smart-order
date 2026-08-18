import { Card } from "@/components/ui/Card";

export interface CartSummaryProps {
  subtotal: number;
  couponDiscount: number;
  stampDiscount: number;
  total: number;
}

/** 주문 금액/할인/총 결제 금액 요약 (F-03) */
export function CartSummary({ subtotal, couponDiscount, stampDiscount, total }: CartSummaryProps) {
  return (
    <Card className="space-y-2">
      <div className="flex justify-between text-sm text-foreground/70">
        <span>주문 금액</span>
        <span>{subtotal.toLocaleString()}원</span>
      </div>
      {couponDiscount > 0 && (
        <div className="flex justify-between text-sm text-accent-text">
          <span>쿠폰 할인</span>
          <span>-{couponDiscount.toLocaleString()}원</span>
        </div>
      )}
      {stampDiscount > 0 && (
        <div className="flex justify-between text-sm text-accent-text">
          <span>스탬프 리워드</span>
          <span>-{stampDiscount.toLocaleString()}원</span>
        </div>
      )}
      <div className="flex justify-between border-t border-border pt-2 text-base font-bold text-foreground">
        <span>총 결제 금액</span>
        <span>{total.toLocaleString()}원</span>
      </div>
    </Card>
  );
}
