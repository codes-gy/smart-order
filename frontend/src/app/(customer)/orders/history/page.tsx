import type { Metadata } from "next";
import { OrderHistoryRouter } from "@/routers/OrderHistoryRouter";

export const metadata: Metadata = {
  title: "주문내역",
  robots: { index: false, follow: false },
};

export default function OrderHistoryPage() {
  return <OrderHistoryRouter />;
}
