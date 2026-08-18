import Link from "next/link";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { StoreThumbnail } from "@/components/store/StoreThumbnail";
import type { StoreSummary } from "@/types/store.types";

export function StoreListItem({ store }: { store: StoreSummary }) {
  return (
    <Link href={`/store/${store.id}`} className="block">
      <Card className="flex gap-3">
        <StoreThumbnail name={store.name} className="h-16 w-16 shrink-0 text-2xl" />
        <div className="min-w-0 flex-1 space-y-1">
          <div className="flex items-center justify-between gap-2">
            <p className="truncate font-semibold text-foreground">{store.name}</p>
            {!store.isOpen && <Badge variant="neutral">영업 종료</Badge>}
          </div>
          <p className="truncate text-sm text-foreground/60">{store.address}</p>
          <div className="flex flex-wrap gap-1.5">
            <Badge variant={store.waitingOrderCount > 3 ? "warning" : "neutral"}>
              대기 {store.waitingOrderCount}건
            </Badge>
            <Badge variant="accent">예상 {store.estimatedPrepMinutes}분</Badge>
            <Badge variant="neutral">{(store.distanceMeters / 1000).toFixed(1)}km</Badge>
          </div>
        </div>
      </Card>
    </Link>
  );
}
