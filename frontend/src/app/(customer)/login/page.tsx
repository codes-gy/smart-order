import type { Metadata } from "next";
import { LoginRouter } from "@/routers/LoginRouter";

export const metadata: Metadata = {
  title: "로그인",
  robots: { index: false, follow: false },
};

export default function LoginPage() {
  return <LoginRouter />;
}
