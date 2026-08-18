import { Skeleton } from "@/components/ui/Skeleton";

export default function OrderHistoryLoading() {
  return (
    <div className="flex min-h-screen flex-col gap-3 p-4">
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-24 w-full" />
      <Skeleton className="h-24 w-full" />
    </div>
  );
}
