import { Skeleton, SkeletonText } from "@/components/ui/Skeleton";

export default function LoginLoading() {
  return (
    <div className="flex min-h-screen flex-col gap-4 p-6">
      <Skeleton className="h-14 w-full" />
      <SkeletonText lines={2} />
      <Skeleton className="h-12 w-full" />
      <Skeleton className="h-12 w-full" />
    </div>
  );
}
