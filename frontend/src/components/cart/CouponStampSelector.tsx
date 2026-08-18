"use client";

import { useQuery } from "@tanstack/react-query";
import { Card } from "@/components/ui/Card";
import { Switch } from "@/components/ui/Switch";
import { Skeleton } from "@/components/ui/Skeleton";
import { authApi } from "@/api/authApi";
import type { Coupon } from "@/types/auth.types";

export interface CouponStampSelectorProps {
  couponId: string | null;
  onCouponChange: (couponId: string | null) => void;
  useStamp: boolean;
  onUseStampChange: (useStamp: boolean) => void;
}

/** 쿠폰/스탬프 리워드 선택 (F-03). 스탬프는 목표치를 채운 경우에만 사용할 수 있다 */
export function CouponStampSelector({
  couponId,
  onCouponChange,
  useStamp,
  onUseStampChange,
}: CouponStampSelectorProps) {
  const couponsQuery = useQuery({ queryKey: ["auth", "coupons"], queryFn: () => authApi.coupons() });
  const rewardsQuery = useQuery({ queryKey: ["auth", "rewards"], queryFn: () => authApi.rewards() });

  const coupons: Coupon[] = couponsQuery.data ?? [];
  const rewards = rewardsQuery.data;
  const stampReady = Boolean(rewards && rewards.stampCount >= rewards.stampGoal);

  return (
    <Card className="space-y-4">
      <div>
        <p className="mb-2 text-sm font-semibold text-foreground">쿠폰</p>
        {couponsQuery.isPending ? (
          <Skeleton className="h-11 w-full" />
        ) : couponsQuery.isError ? (
          <p className="text-sm text-danger">쿠폰 목록을 불러오지 못했어요.</p>
        ) : coupons.length === 0 ? (
          <p className="text-sm text-foreground/70">사용 가능한 쿠폰이 없어요.</p>
        ) : (
          <div className="flex flex-col gap-2" role="radiogroup" aria-label="쿠폰 선택">
            <label className="flex h-11 items-center gap-2 rounded-xl border border-border px-3 text-sm text-foreground">
              <input
                type="radio"
                name="coupon"
                checked={couponId === null}
                onChange={() => onCouponChange(null)}
                className="h-4 w-4"
              />
              사용 안 함
            </label>
            {coupons.map((coupon) => (
              <label
                key={coupon.id}
                className="flex h-11 items-center justify-between gap-2 rounded-xl border border-border px-3 text-sm text-foreground"
              >
                <span className="flex min-w-0 items-center gap-2">
                  <input
                    type="radio"
                    name="coupon"
                    checked={couponId === coupon.id}
                    onChange={() => onCouponChange(coupon.id)}
                    className="h-4 w-4 shrink-0"
                  />
                  <span className="truncate">{coupon.name}</span>
                </span>
                <span className="shrink-0 font-semibold text-accent-text">
                  -{coupon.discountAmount.toLocaleString()}원
                </span>
              </label>
            ))}
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-3 border-t border-border pt-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-foreground">스탬프 리워드 사용</p>
          <p className="truncate text-xs text-foreground/70">
            {rewardsQuery.isPending
              ? "불러오는 중..."
              : stampReady
                ? "스탬프 10개를 모두 모았어요."
                : `스탬프 ${rewards?.stampCount ?? 0}/${rewards?.stampGoal ?? 10}개`}
          </p>
        </div>
        <Switch
          checked={useStamp}
          onCheckedChange={onUseStampChange}
          disabled={!stampReady}
          label="스탬프 리워드 사용"
        />
      </div>
    </Card>
  );
}
