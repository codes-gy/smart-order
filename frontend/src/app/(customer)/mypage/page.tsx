import type { Metadata } from "next";
import { MyPageRouter } from "@/routers/MyPageRouter";

export const metadata: Metadata = {
  title: "마이페이지",
  robots: { index: false, follow: false },
};

export default function MyPagePage() {
  return <MyPageRouter />;
}
