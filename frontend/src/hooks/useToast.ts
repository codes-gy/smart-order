"use client";

import { useContext } from "react";
import { ToastContext, type ToastContextValue } from "@/components/ui/Toast";

/** ToastProvider 하위에서 토스트를 띄우고 닫기 위한 훅 */
export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast는 ToastProvider 내부에서만 사용할 수 있습니다.");
  }
  return context;
}
