import type { Metadata } from "next";
import { CartRouter } from "@/routers/CartRouter";

export const metadata: Metadata = {
  title: "장바구니",
  robots: { index: false, follow: false },
};

export default function CartPage() {
  return <CartRouter />;
}
