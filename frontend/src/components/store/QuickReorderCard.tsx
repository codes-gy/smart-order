import Link from "next/link";
import { Card } from "@/components/ui/Card";
import type { QuickReorderItem } from "@/types/store.types";

/** 최근 주문 내역 기반 원클릭 퀵 재주문 카드 (F-01) */
export function QuickReorderCard({ item }: { item: QuickReorderItem }) {
  return (
    <Link href={`/store/${item.storeId}`} className="block shrink-0">
      <Card padding="sm" className="w-48 space-y-1">
        <p className="truncate text-sm font-semibold text-foreground">{item.storeName}</p>
        <p className="truncate text-xs text-foreground/60">{item.menuSummary}</p>
        <p className="text-xs font-medium text-accent-text">한 번에 다시 주문</p>
      </Card>
    </Link>
  );
}
