"use client";

import { useCallback, useState, useSyncExternalStore } from "react";

function subscribe(): () => void {
  // Notification.permission 변경을 구독할 수 있는 네이티브 이벤트가 없어 no-op으로 둔다.
  // 최초 마운트 시 서버/클라이언트 스냅샷 불일치(hydration mismatch)만 방지하면 된다.
  return () => {};
}

function getSnapshot(): NotificationPermission | "unsupported" {
  if (typeof window === "undefined" || !("Notification" in window)) return "unsupported";
  return Notification.permission;
}

function getServerSnapshot(): NotificationPermission | "unsupported" {
  return "unsupported";
}

export interface UsePushNotificationPermissionResult {
  isSupported: boolean;
  permission: NotificationPermission | "unsupported";
  requestPermission: () => void;
}

/**
 * 브라우저 Notification API 권한 상태 훅.
 * 실제 푸시 서버(Web Push) 없이도 픽업 준비 완료 시 포그라운드 알림을 데모할 수 있도록 한다.
 */
export function usePushNotificationPermission(): UsePushNotificationPermissionResult {
  const initialPermission = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  // requestPermission()의 비동기 결과는 override로 즉시 반영한다 (콜백 내부 호출이라 set-state-in-effect 규칙과 무관).
  const [override, setOverride] = useState<NotificationPermission | null>(null);
  const isSupported = initialPermission !== "unsupported";

  const requestPermission = useCallback(() => {
    if (typeof window === "undefined" || !("Notification" in window)) return;
    Notification.requestPermission().then((result) => setOverride(result));
  }, []);

  return {
    isSupported,
    permission: override ?? initialPermission,
    requestPermission,
  };
}
