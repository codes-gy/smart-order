"use client";

import { Component, type ErrorInfo, type ReactNode } from "react";
import { ErrorAlert } from "@/components/ui/ErrorAlert";

export interface ErrorBoundaryProps {
  children: ReactNode;
  /** 커스텀 폴백 UI. 미지정 시 기본 ErrorAlert(layout="page")를 사용한다 */
  fallback?: (args: { error: Error; reset: () => void }) => ReactNode;
  /** 에러 발생 시 로깅 훅. Phase 8에서 Sentry 연동 지점으로 사용 예정 */
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface ErrorBoundaryState {
  error: Error | null;
}

/**
 * 렌더링 중 발생하는 런타임 에러를 잡아 화면 전체가 하얗게 죽는 것을 방지하는 클래스 컴포넌트.
 * Next.js App Router의 error.tsx는 세그먼트 단위 에러만 잡으므로,
 * 특정 위젯(예: 칸반 카드 하나)을 감싸는 국소적 방어용으로 함께 사용한다.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    this.props.onError?.(error, errorInfo);
    // Phase 8에서 Sentry.captureException으로 교체 예정
    console.error("[ErrorBoundary]", error, errorInfo);
  }

  reset = (): void => {
    this.setState({ error: null });
  };

  render(): ReactNode {
    const { error } = this.state;
    if (!error) return this.props.children;

    if (this.props.fallback) {
      return this.props.fallback({ error, reset: this.reset });
    }

    return (
      <ErrorAlert
        layout="page"
        title="화면을 불러오지 못했어요"
        description={error.message || "잠시 후 다시 시도해주세요."}
        onRetry={this.reset}
      />
    );
  }
}
