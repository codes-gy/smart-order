"use client";

import Link from "next/link";
import { IconShoppingCart } from "@/components/ui/icons";
import { useCart } from "@/hooks/useCart";

/** 헤더에 삽입되는 장바구니 바로가기 버튼. 담긴 수량을 배지로 보여준다 (F-03) */
export function CartBadgeLink() {
  const cart = useCart();

  return (
    <Link
      href="/cart"
      aria-label={cart.itemCount > 0 ? `장바구니, ${cart.itemCount}개 담김` : "장바구니"}
      className="relative flex h-12 w-12 items-center justify-center rounded-full hover:bg-surface-muted"
    >
      <IconShoppingCart />
      {cart.itemCount > 0 && (
        <span
          aria-hidden="true"
          className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-accent px-1 text-[10px] font-bold leading-none text-accent-foreground"
        >
          {cart.itemCount > 99 ? "99+" : cart.itemCount}
        </span>
      )}
    </Link>
  );
}
