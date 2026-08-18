import type { Metadata } from "next";
import { StoreAdminLoginRouter } from "@/routers/StoreAdminLoginRouter";

export const metadata: Metadata = {
  title: "매장 관리자 로그인",
  robots: { index: false, follow: false },
};

export default function StoreAdminLoginPage() {
  return <StoreAdminLoginRouter />;
}
