"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { ToastProvider } from "@/components/ui/Toast";
import { ErrorBoundary } from "@/components/ui/ErrorBoundary";

/**
 * 앱 전역 Provider 모음.
 * - ErrorBoundary: 최상위 방어선. 세그먼트별 error.tsx는 각 라우트에서 별도로 둔다.
 * - TanStack Query: 서버 데이터(매장/메뉴/주문 등) 캐싱 및 동기화
 * - ToastProvider: 전역 토스트 알림(F-02 품절 토스트, F-06 픽업 알림 등)
 */
export function Providers({ children }: { children: ReactNode }): React.JSX.Element {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            gcTime: 5 * 60 * 1000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>{children}</ToastProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
