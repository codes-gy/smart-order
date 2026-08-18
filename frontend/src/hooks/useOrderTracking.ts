"use client";

import { useEffect, useRef, useState } from "react";
import { orderTrackingApi } from "@/api/orderTrackingApi";
import { POLLING_INTERVAL_MS, SSE_MAX_RECONNECT_ATTEMPTS, SSE_RECONNECT_DELAYS_MS } from "@/utils/constants";
import type { ConnectionStatus } from "@/types/common.types";
import type { OrderTrackingEvent } from "@/types/order.types";

export interface UseOrderTrackingResult {
  connectionStatus: ConnectionStatus;
  latestEvent: OrderTrackingEvent | null;
  events: OrderTrackingEvent[];
}

/**
 * 주문 실시간 추적(F-04): SSE로 연결을 시도하고, 끊기면 PRD 4.1의 Exponential Backoff
 * (1s/2s/4s/8s)로 재연결을 시도한다. SSE_MAX_RECONNECT_ATTEMPTS(3회)를 넘겨 실패하면
 * REST Polling(POLLING_INTERVAL_MS 주기)으로 폴백한다.
 */
export function useOrderTracking(orderId: string): UseOrderTrackingResult {
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>("connecting");
  const [events, setEvents] = useState<OrderTrackingEvent[]>([]);

  const sourceRef = useRef<{ close: () => void } | null>(null);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pollingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const attemptRef = useRef(0);

  useEffect(() => {
    let cancelled = false;

    const clearReconnectTimer = () => {
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
    };
    const clearPollingTimer = () => {
      if (pollingTimerRef.current) {
        clearInterval(pollingTimerRef.current);
        pollingTimerRef.current = null;
      }
    };
    const pushEvent = (event: OrderTrackingEvent) => {
      if (cancelled) return;
      setEvents((prev) => (prev.at(-1)?.status === event.status ? prev : [...prev, event]));
    };

    const startPolling = () => {
      clearReconnectTimer();
      sourceRef.current?.close();
      setConnectionStatus("polling");

      const poll = () => {
        orderTrackingApi
          .fetchStatus(orderId)
          .then(pushEvent)
          .catch(() => {
            // REST 폴링 실패는 다음 주기에 재시도한다 (조용히 무시)
          });
      };
      poll();
      pollingTimerRef.current = setInterval(poll, POLLING_INTERVAL_MS);
    };

    const connect = () => {
      clearPollingTimer();

      sourceRef.current = orderTrackingApi.openEventSource(orderId, {
        onOpen: () => {
          if (cancelled) return;
          attemptRef.current = 0;
          setConnectionStatus("open");
        },
        onMessage: (event) => {
          pushEvent(event);
        },
        onError: () => {
          if (cancelled) return;
          sourceRef.current?.close();
          attemptRef.current += 1;

          if (attemptRef.current > SSE_MAX_RECONNECT_ATTEMPTS) {
            startPolling();
            return;
          }

          setConnectionStatus("reconnecting");
          const delay =
            SSE_RECONNECT_DELAYS_MS[attemptRef.current - 1] ??
            SSE_RECONNECT_DELAYS_MS[SSE_RECONNECT_DELAYS_MS.length - 1];
          reconnectTimerRef.current = setTimeout(connect, delay);
        },
      });
    };

    connect();

    return () => {
      cancelled = true;
      clearReconnectTimer();
      clearPollingTimer();
      sourceRef.current?.close();
    };
  }, [orderId]);

  return {
    connectionStatus,
    latestEvent: events.at(-1) ?? null,
    events,
  };
}
