"use client";

import {
  createContext,
  useCallback,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import { useEffect, useRef } from "react";
import {
  IconAlertTriangle,
  IconCheckCircle,
  IconInfo,
  IconX,
  IconXCircle,
} from "@/components/ui/icons";
import { useIsClient } from "@/hooks/useIsClient";
import { cn } from "@/utils/cn";

export type ToastVariant = "success" | "error" | "warning" | "info";

export interface ToastInput {
  variant?: ToastVariant;
  title: string;
  description?: string;
  /** ms. 0을 넘기면 자동으로 사라지지 않는다 (예: SSE 재연결 실패 경고) */
  duration?: number;
}

export interface ToastItem extends Required<Pick<ToastInput, "title" | "variant" | "duration">> {
  id: string;
  description?: string;
}

export interface ToastContextValue {
  toasts: ToastItem[];
  show: (toast: ToastInput) => string;
  dismiss: (id: string) => void;
}

export const ToastContext = createContext<ToastContextValue | null>(null);

const DEFAULT_DURATION_MS = 3500;

const variantStyle: Record<ToastVariant, { icon: ReactNode; className: string }> = {
  success: {
    icon: <IconCheckCircle className="h-5 w-5 text-success" />,
    className: "border-success/30",
  },
  error: {
    icon: <IconXCircle className="h-5 w-5 text-danger" />,
    className: "border-danger/30",
  },
  warning: {
    icon: <IconAlertTriangle className="h-5 w-5 text-warning" />,
    className: "border-warning/30",
  },
  info: {
    icon: <IconInfo className="h-5 w-5 text-accent" />,
    className: "border-accent/30",
  },
};

function ToastCard({ toast, onDismiss }: { toast: ToastItem; onDismiss: (id: string) => void }) {
  const style = variantStyle[toast.variant];

  return (
    <div
      role={toast.variant === "error" ? "alert" : "status"}
      className={cn(
        "pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-xl border bg-surface p-4 shadow-lg",
        style.className,
      )}
    >
      <div className="mt-0.5 shrink-0">{style.icon}</div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-foreground">{toast.title}</p>
        {toast.description && (
          <p className="mt-0.5 text-sm text-foreground/70">{toast.description}</p>
        )}
      </div>
      <button
        type="button"
        onClick={() => onDismiss(toast.id)}
        aria-label="알림 닫기"
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-foreground/50 hover:bg-surface-muted"
      >
        <IconX className="h-4 w-4" />
      </button>
    </div>
  );
}

function ToastViewport({ toasts, onDismiss }: { toasts: ToastItem[]; onDismiss: (id: string) => void }) {
  const mounted = useIsClient();

  if (!mounted) return null;

  return createPortal(
    <div
      aria-live="polite"
      aria-atomic="false"
      className="pointer-events-none fixed inset-x-0 top-[calc(env(safe-area-inset-top)+12px)] z-[100] flex flex-col items-center gap-2 px-4 sm:top-4"
    >
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} onDismiss={onDismiss} />
      ))}
    </div>,
    document.body,
  );
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const timers = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  const dismiss = useCallback((id: string) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
    const timer = timers.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timers.current.delete(id);
    }
  }, []);

  const show = useCallback(
    (input: ToastInput) => {
      const id = crypto.randomUUID();
      const duration = input.duration ?? DEFAULT_DURATION_MS;
      const toast: ToastItem = {
        id,
        title: input.title,
        description: input.description,
        variant: input.variant ?? "info",
        duration,
      };

      setToasts((current) => [...current, toast]);

      if (duration > 0) {
        const timer = setTimeout(() => dismiss(id), duration);
        timers.current.set(id, timer);
      }

      return id;
    },
    [dismiss],
  );

  useEffect(() => {
    const timersMap = timers.current;
    return () => {
      timersMap.forEach((timer) => clearTimeout(timer));
      timersMap.clear();
    };
  }, []);

  const value = useMemo<ToastContextValue>(() => ({ toasts, show, dismiss }), [toasts, show, dismiss]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}
