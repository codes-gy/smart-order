import type { Metadata } from "next";
import { CheckoutRouter } from "@/routers/CheckoutRouter";

export const metadata: Metadata = {
  title: "결제하기",
  robots: { index: false, follow: false },
};

export default function CheckoutPage() {
  return <CheckoutRouter />;
}
