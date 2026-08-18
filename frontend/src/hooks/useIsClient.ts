"use client";

import { useSyncExternalStore } from "react";

function subscribe(): () => void {
  return () => {};
}

function getClientSnapshot(): boolean {
  return true;
}

function getServerSnapshot(): boolean {
  return false;
}

/**
 * 서버 렌더링 시 false, 클라이언트 하이드레이션 이후 true를 반환한다.
 * createPortal처럼 document가 필요한 컴포넌트에서 SSR 불일치 없이 마운트 여부를 판단할 때 사용.
 * (useEffect + setState 패턴은 react-hooks/set-state-in-effect 룰에 걸리므로 useSyncExternalStore로 대체)
 */
export function useIsClient(): boolean {
  return useSyncExternalStore(subscribe, getClientSnapshot, getServerSnapshot);
}
