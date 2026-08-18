"use client";

import { useEffect } from "react";

/**
 * 프로덕션 빌드에서만 서비스워커를 등록한다.
 * 정적 자산은 Cache First, 페이지 탐색은 Network First(오프라인 시 캐시 폴백)로 동작해
 * 재방문 시 오프라인 대응과 홈 화면 추가(PWA 설치)를 지원한다.
 */
export function ServiceWorkerRegister() {
  useEffect(() => {
    if (process.env.NODE_ENV !== "production") return;
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) return;

    navigator.serviceWorker.register("/sw.js").catch(() => {
      // 서비스워커 등록 실패는 앱 핵심 기능에 영향을 주지 않으므로 조용히 무시한다.
    });
  }, []);

  return null;
}
