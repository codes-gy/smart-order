import type { Metadata } from "next";
import { OrderTrackingRouter } from "@/routers/OrderTrackingRouter";

export const metadata: Metadata = {
  title: "주문 현황",
  robots: { index: false, follow: false },
};

export default async function OrderTrackingPage({
  params,
}: PageProps<"/orders/[orderId]/track">) {
  const { orderId } = await params;
  return <OrderTrackingRouter orderId={orderId} />;
}
