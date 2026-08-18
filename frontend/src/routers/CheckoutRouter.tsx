"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Header } from "@/components/ui/layout/Header";
import { Button } from "@/components/ui/Button";
import { ErrorAlert } from "@/components/ui/ErrorAlert";
import { Skeleton } from "@/components/ui/Skeleton";
import { CartSummary } from "@/components/cart/CartSummary";
import { PaymentMethodSelector } from "@/components/cart/PaymentMethodSelector";
import { CheckoutOverlay } from "@/components/cart/CheckoutOverlay";
import { useCheckout } from "@/hooks/useCheckout";
import { ApiError } from "@/types/common.types";

/** 결제하기(F-03) 페이지 조립: 재검증 -> 결제 수단 선택 -> 결제 처리/완료 */
export function CheckoutRouter() {
  const router = useRouter();
  const checkout = useCheckout();
  const isEmpty = checkout.cart.items.length === 0;

  useEffect(() => {
    if (isEmpty && checkout.step === "review") {
      router.replace("/cart");
    }
  }, [isEmpty, checkout.step, router]);

  if (isEmpty && checkout.step === "review") {
    return null;
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Header title="결제하기" onBack={() => router.back()} />

      <main id="main-content" className="flex-1 space-y-4 p-4 pb-32">
        {checkout.cart.storeName && (
          <p className="text-sm font-semibold text-foreground/70">{checkout.cart.storeName}</p>
        )}

        <div className="space-y-1.5">
          {checkout.cart.items.map((line) => (
            <div key={line.lineId} className="flex justify-between text-sm text-foreground/80">
              <span className="truncate pr-2">
                {line.menuName} x{line.quantity}
              </span>
              <span className="shrink-0">{(line.unitPrice * line.quantity).toLocaleString()}원</span>
            </div>
          ))}
        </div>

        {checkout.isValidating && <Skeleton className="h-12 w-full" />}

        {!checkout.isValidating && !checkout.isOrderValid && checkout.issues.length > 0 && (
          <ErrorAlert
            title="주문 내용을 확인해주세요"
            description={checkout.issues.map((issue) => issue.message).join(" / ")}
            onRetry={checkout.revalidate}
            retryLabel="다시 확인"
          />
        )}

        <div>
          <p className="mb-2 text-sm font-semibold text-foreground">결제 수단</p>
          <PaymentMethodSelector value={checkout.paymentMethod} onChange={checkout.setPaymentMethod} />
        </div>

        <CartSummary
          subtotal={checkout.cart.subtotal}
          couponDiscount={checkout.couponDiscount}
          stampDiscount={checkout.stampDiscount}
          total={checkout.estimatedTotal}
        />

        {checkout.isError && (
          <ErrorAlert
            description={
              checkout.error instanceof ApiError
                ? checkout.error.message
                : "결제에 실패했어요. 다시 시도해주세요."
            }
            onRetry={checkout.submit}
          />
        )}
      </main>

      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-background p-4 pb-[calc(env(safe-area-inset-bottom)+1rem)]">
        <Button
          type="button"
          size="lg"
          fullWidth
          disabled={!checkout.isOrderValid || checkout.isValidating}
          isLoading={checkout.isSubmitting}
          loadingText="결제 중"
          onClick={checkout.submit}
        >
          {checkout.estimatedTotal.toLocaleString()}원 결제하기
        </Button>
      </div>

      {checkout.step !== "review" && (
        <CheckoutOverlay
          status={checkout.step === "processing" ? "processing" : "success"}
          orderId={checkout.completedOrderId}
          totalAmount={checkout.completedTotal}
          onConfirm={checkout.viewTracking}
        />
      )}
    </div>
  );
}
