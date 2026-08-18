import type { Metadata } from "next";
import { StoreAdminDashboardRouter } from "@/routers/StoreAdminDashboardRouter";

export const metadata: Metadata = {
  title: "매장 관리자 대시보드",
  robots: { index: false, follow: false },
};

export default function StoreAdminDashboardPage() {
  return <StoreAdminDashboardRouter />;
}
