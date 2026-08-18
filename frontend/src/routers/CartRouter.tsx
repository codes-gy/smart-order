"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Header } from "@/components/ui/layout/Header";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { IconShoppingCart } from "@/components/ui/icons";
import { CartItem } from "@/components/cart/CartItem";
import { CartSummary } from "@/components/cart/CartSummary";
import { PackagingSelector } from "@/components/cart/PackagingSelector";
import { CouponStampSelector } from "@/components/cart/CouponStampSelector";
import { useCart } from "@/hooks/useCart";
import { getCouponDiscountAmount, STAMP_REWARD_DISCOUNT } from "@/api/mock/rewardsMock";

/** 장바구니(F-03) 페이지 조립 */
export function CartRouter() {
  const router = useRouter();
  const cart = useCart();

  const couponDiscount = getCouponDiscountAmount(cart.couponId);
  const stampDiscount = cart.useStamp ? STAMP_REWARD_DISCOUNT : 0;
  const total = Math.max(0, cart.subtotal - couponDiscount - stampDiscount);
  const isEmpty = cart.items.length === 0;

  return (
    <div className="flex min-h-screen flex-col">
      <Header title="장바구니" onBack={() => router.back()} />

      {isEmpty ? (
        <EmptyState
          icon={<IconShoppingCart className="h-10 w-10" />}
          title="장바구니가 비어있어요"
          description="매장에서 메뉴를 담아보세요."
          action={
            <Link href="/">
              <Button type="button">매장 둘러보기</Button>
            </Link>
          }
        />
      ) : (
        <main id="main-content" className="flex-1 space-y-4 p-4 pb-32">
          {cart.storeName && <p className="text-sm font-semibold text-foreground/70">{cart.storeName}</p>}

          <div className="space-y-3">
            {cart.items.map((line) => (
              <CartItem key={line.lineId} line={line} onQuantityChange={cart.setQuantity} onRemove={cart.removeItem} />
            ))}
          </div>

          <div>
            <p className="mb-2 text-sm font-semibold text-foreground">포장 방식</p>
            <PackagingSelector value={cart.packagingType} onChange={cart.setPackagingType} />
          </div>

          <CouponStampSelector
            couponId={cart.couponId}
            onCouponChange={cart.setCouponId}
            useStamp={cart.useStamp}
            onUseStampChange={cart.setUseStamp}
          />

          <CartSummary subtotal={cart.subtotal} couponDiscount={couponDiscount} stampDiscount={stampDiscount} total={total} />
        </main>
      )}

      {!isEmpty && (
        <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-background p-4 pb-[calc(env(safe-area-inset-bottom)+1rem)]">
          <Button type="button" size="lg" fullWidth onClick={() => router.push("/checkout")}>
            {total.toLocaleString()}원 결제하기
          </Button>
        </div>
      )}
    </div>
  );
}
