import { Skeleton, SkeletonText } from "@/components/ui/Skeleton";

export default function MyPageLoading() {
  return (
    <div className="flex min-h-screen flex-col gap-4 p-4">
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-24 w-full" />
      <SkeletonText lines={3} />
    </div>
  );
}
