import type { Metadata } from "next";
import { StoreFinderRouter } from "@/routers/StoreFinderRouter";

export const metadata: Metadata = {
  title: "매장 찾기",
};

export default function HomePage() {
  return <StoreFinderRouter />;
}
