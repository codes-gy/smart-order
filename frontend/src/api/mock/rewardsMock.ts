import { STAMP_REWARD_DISCOUNT } from "@/utils/constants";
import type { Coupon } from "@/types/auth.types";

/**
 * 쿠폰/스탬프 Mock 데이터. authMock(쿠폰 목록 조회)과 orderMock(주문 금액 계산)이
 * 동일한 데이터를 참조해야 클라이언트 화면과 서버(mock) 계산 결과가 어긋나지 않는다.
 */
export const MOCK_COUPONS: Coupon[] = [
  {
    id: "coupon-1000",
    name: "1,000원 할인 쿠폰",
    discountAmount: 1000,
    expiresAt: new Date(Date.now() + 14 * 86_400_000).toISOString(),
  },
  {
    id: "coupon-welcome",
    name: "웰컴 2,000원 할인 쿠폰",
    discountAmount: 2000,
    expiresAt: new Date(Date.now() + 30 * 86_400_000).toISOString(),
  },
];

export function getCouponDiscountAmount(couponId: string | null): number {
  if (!couponId) return 0;
  return MOCK_COUPONS.find((coupon) => coupon.id === couponId)?.discountAmount ?? 0;
}

export { STAMP_REWARD_DISCOUNT };
