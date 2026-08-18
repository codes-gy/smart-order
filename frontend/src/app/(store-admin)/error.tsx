"use client";

import { useEffect } from "react";
import { ErrorAlert } from "@/components/ui/ErrorAlert";

export default function StoreAdminSegmentError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Phase 8에서 Sentry.captureException(error)로 교체 예정
    console.error("[StoreAdminSegmentError]", error);
  }, [error]);

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <ErrorAlert
        layout="page"
        title="화면을 불러오지 못했어요"
        description={error.message || "잠시 후 다시 시도해주세요."}
        onRetry={reset}
      />
    </div>
  );
}
