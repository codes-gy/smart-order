"use client";

import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { orderApi } from "@/api/orderApi";
import { paymentApi } from "@/api/paymentApi";
import { getCouponDiscountAmount, STAMP_REWARD_DISCOUNT } from "@/api/mock/rewardsMock";
import { useCart, type UseCartResult } from "@/hooks/useCart";
import { createIdempotencyKey } from "@/utils/idempotency";
import { ApiError } from "@/types/common.types";
import type { PaymentMethod } from "@/types/payment.types";
import type { OrderLineItemInput, ValidateOrderIssue } from "@/types/order.types";

export type CheckoutStep = "review" | "processing" | "success";

export interface UseCheckoutResult {
  cart: UseCartResult;
  paymentMethod: PaymentMethod;
  setPaymentMethod: (method: PaymentMethod) => void;
  step: CheckoutStep;
  isValidating: boolean;
  isOrderValid: boolean;
  issues: ValidateOrderIssue[];
  revalidate: () => void;
  couponDiscount: number;
  stampDiscount: number;
  estimatedTotal: number;
  submit: () => void;
  isSubmitting: boolean;
  isError: boolean;
  error: unknown;
  completedOrderId: string | null;
  completedTotal: number | null;
  goToHome: () => void;
  viewTracking: () => void;
}

/** 결제하기(F-03) 흐름: 재검증 -> 주문 생성(멱등) -> 결제 승인 -> 완료 */
export function useCheckout(): UseCheckoutResult {
  const router = useRouter();
  const cart = useCart();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("KAKAO_PAY");
  const [step, setStep] = useState<CheckoutStep>("review");
  const [completedOrderId, setCompletedOrderId] = useState<string | null>(null);
  const [completedTotal, setCompletedTotal] = useState<number | null>(null);
  // 결제 재시도 시에도 같은 키를 재사용해야 서버(mock)가 중복 주문을 만들지 않는다 (X-Idempotency-Key).
  const idempotencyKeyRef = useRef<string | null>(null);

  const orderItems: OrderLineItemInput[] = useMemo(
    () =>
      cart.items.map((line) => ({
        menuId: line.menuId,
        quantity: line.quantity,
        optionChoiceIds: line.selections.flatMap((selection) => selection.choiceIds),
      })),
    [cart.items],
  );

  const validateQuery = useQuery({
    queryKey: ["orders", "validate", cart.storeId, orderItems],
    queryFn: () => orderApi.validate({ storeId: cart.storeId as string, items: orderItems }),
    enabled: Boolean(cart.storeId) && orderItems.length > 0,
  });

  const couponDiscount = getCouponDiscountAmount(cart.couponId);
  const stampDiscount = cart.useStamp ? STAMP_REWARD_DISCOUNT : 0;
  const estimatedTotal = Math.max(0, cart.subtotal - couponDiscount - stampDiscount);

  const checkoutMutation = useMutation({
    mutationFn: async () => {
      if (!cart.storeId) {
        throw new ApiError(400, { code: "EMPTY_CART", message: "장바구니가 비어있어요." });
      }
      if (!idempotencyKeyRef.current) {
        idempotencyKeyRef.current = createIdempotencyKey();
      }

      const order = await orderApi.create({
        storeId: cart.storeId,
        items: orderItems,
        packagingType: cart.packagingType,
        couponId: cart.couponId,
        useStamp: cart.useStamp,
        idempotencyKey: idempotencyKeyRef.current,
      });

      const confirmation = await paymentApi.confirm({
        orderId: order.orderId,
        paymentKey: `mock-payment-${crypto.randomUUID()}`,
        amount: order.totalAmount,
      });

      return { order, confirmation };
    },
    onMutate: () => setStep("processing"),
    onSuccess: ({ order }) => {
      setCompletedOrderId(order.orderId);
      setCompletedTotal(order.totalAmount);
      setStep("success");
      idempotencyKeyRef.current = null;
      cart.clear();
    },
    onError: () => {
      // idempotencyKeyRef는 초기화하지 않는다: 재시도 시 동일 키로 요청해 멱등성을 지킨다.
      setStep("review");
    },
  });

  return {
    cart,
    paymentMethod,
    setPaymentMethod,
    step,
    isValidating: validateQuery.isPending,
    isOrderValid: validateQuery.data?.isValid ?? true,
    issues: validateQuery.data?.issues ?? [],
    revalidate: () => validateQuery.refetch(),
    couponDiscount,
    stampDiscount,
    estimatedTotal,
    submit: () => checkoutMutation.mutate(),
    isSubmitting: checkoutMutation.isPending,
    isError: checkoutMutation.isError,
    error: checkoutMutation.error,
    completedOrderId,
    completedTotal,
    goToHome: () => router.push("/"),
    viewTracking: () => router.push(completedOrderId ? `/orders/${completedOrderId}/track` : "/"),
  };
}
