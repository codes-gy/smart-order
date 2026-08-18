import { Skeleton, SkeletonText } from "@/components/ui/Skeleton";

export default function CheckoutLoading() {
  return (
    <div className="flex min-h-screen flex-col gap-4 p-4">
      <Skeleton className="h-14 w-full" />
      <SkeletonText lines={3} />
      <Skeleton className="h-32 w-full" />
    </div>
  );
}
