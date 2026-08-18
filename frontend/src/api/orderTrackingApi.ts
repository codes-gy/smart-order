import { mockFetchOrderStatus, openMockOrderEventSource } from "@/api/mock/orderTrackingMock";
import type { OrderTrackingEvent } from "@/types/order.types";

export interface OrderTrackingEventSourceHandlers {
  onOpen?: () => void;
  onMessage: (event: OrderTrackingEvent) => void;
  onError: () => void;
}

/**
 * 주문 추적 도메인 API 파사드. 현재는 Mock 구현에 위임한다 (PRD 4.4, 6.B).
 * openEventSource는 실제 연동 시 `new EventSource(...)` 기반 구현으로 교체될 지점이다.
 */
export const orderTrackingApi = {
  fetchStatus: (orderId: string): Promise<OrderTrackingEvent> => mockFetchOrderStatus(orderId),
  openEventSource: (orderId: string, handlers: OrderTrackingEventSourceHandlers): { close: () => void } =>
    openMockOrderEventSource(orderId, handlers),
};
