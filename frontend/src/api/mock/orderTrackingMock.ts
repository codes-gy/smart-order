import { MOCK_API_DELAY_MS } from "@/utils/constants";
import type { OrderStatus, OrderTrackingEvent } from "@/types/order.types";

function delay(ms: number = MOCK_API_DELAY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** 정상 진행 시퀀스. CANCELLED는 별도 분기(시퀀스에 포함하지 않음) */
export const ORDER_STATUS_SEQUENCE: OrderStatus[] = ["PENDING", "ACCEPTED", "PREPARING", "READY", "PICKED_UP"];

const STATUS_MESSAGE: Record<OrderStatus, string> = {
  PENDING: "주문을 접수하고 있어요",
  ACCEPTED: "매장에서 주문을 확인했어요",
  PREPARING: "음료를 제조하고 있어요",
  READY: "픽업 준비가 완료됐어요",
  PICKED_UP: "픽업이 완료됐어요",
  CANCELLED: "주문이 취소됐어요",
};

const statusRegistry = new Map<string, OrderStatus>();
const progressTimers = new Map<string, ReturnType<typeof setInterval>>();

/** 매장 POS가 실제로 주문을 처리하며 상태를 진행시키는 흐름을 흉내낸 "백엔드 측" 타이머 */
function ensureBackendProgress(orderId: string): void {
  if (progressTimers.has(orderId)) return;
  if (!statusRegistry.has(orderId)) statusRegistry.set(orderId, "PENDING");

  const current = statusRegistry.get(orderId);
  const currentIndex = current ? ORDER_STATUS_SEQUENCE.indexOf(current) : -1;
  if (current === "CANCELLED" || currentIndex === ORDER_STATUS_SEQUENCE.length - 1) return;

  const timer = setInterval(() => {
    const status = statusRegistry.get(orderId) ?? "PENDING";
    const index = ORDER_STATUS_SEQUENCE.indexOf(status);
    if (status === "CANCELLED" || index === -1 || index >= ORDER_STATUS_SEQUENCE.length - 1) {
      clearInterval(timer);
      progressTimers.delete(orderId);
      return;
    }
    statusRegistry.set(orderId, ORDER_STATUS_SEQUENCE[index + 1]);
  }, 2500);

  progressTimers.set(orderId, timer);
}

/** 주문 생성 시 호출해 추적 대상으로 등록한다. initialStatus가 종료 상태면 진행 타이머는 시작되지 않는다 */
export function registerOrder(orderId: string, initialStatus: OrderStatus = "PENDING"): void {
  statusRegistry.set(orderId, initialStatus);
  ensureBackendProgress(orderId);
}

export function peekOrderStatus(orderId: string): OrderStatus {
  return statusRegistry.get(orderId) ?? "PENDING";
}

/**
 * 매장 관리자(F-05)가 상태를 직접 변경할 때 사용한다.
 * 이 시점부터는 매장 직원의 조작이 상태 진행의 기준이 되므로, 자동 진행 타이머는 멈춘다.
 */
export function setOrderStatus(orderId: string, status: OrderStatus): void {
  const timer = progressTimers.get(orderId);
  if (timer) {
    clearInterval(timer);
    progressTimers.delete(orderId);
  }
  statusRegistry.set(orderId, status);
}

function buildEvent(orderId: string, status: OrderStatus): OrderTrackingEvent {
  return { orderId, status, updatedAt: new Date().toISOString(), message: STATUS_MESSAGE[status] };
}

/** REST Polling 폴백(PRD 4.1)용 단건 상태 조회 */
export async function mockFetchOrderStatus(orderId: string): Promise<OrderTrackingEvent> {
  await delay(300);
  ensureBackendProgress(orderId);
  return buildEvent(orderId, peekOrderStatus(orderId));
}

export interface MockOrderEventSourceHandlers {
  onOpen?: () => void;
  onMessage: (event: OrderTrackingEvent) => void;
  onError: () => void;
}

/** 주문별로 최초 1회만 인위적인 연결 끊김을 재현해 재연결(backoff) 로직을 실제로 타보게 한다 */
const droppedOnce = new Set<string>();

/**
 * 실제 EventSource(SSE)를 흉내낸 Mock 연결.
 * onOpen/onMessage/onError 콜백 시그니처를 브라우저 EventSource와 맞춰두어,
 * 실제 백엔드 연동 시 이 함수 내부만 `new EventSource(url)` 기반 구현으로 교체하면 되도록 했다.
 */
export function openMockOrderEventSource(
  orderId: string,
  handlers: MockOrderEventSourceHandlers,
): { close: () => void } {
  ensureBackendProgress(orderId);

  let closed = false;
  let tick = 0;
  let lastEmitted: OrderStatus | null = null;

  const openTimer = setTimeout(() => {
    if (!closed) handlers.onOpen?.();
  }, 250);

  const pollTimer = setInterval(() => {
    if (closed) return;
    tick += 1;

    if (tick === 2 && !droppedOnce.has(orderId)) {
      droppedOnce.add(orderId);
      handlers.onError();
      return;
    }

    const status = peekOrderStatus(orderId);
    if (status !== lastEmitted) {
      lastEmitted = status;
      handlers.onMessage(buildEvent(orderId, status));
    }
  }, 900);

  return {
    close: () => {
      closed = true;
      clearTimeout(openTimer);
      clearInterval(pollTimer);
    },
  };
}
