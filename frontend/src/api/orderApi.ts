import {
  mockAdvanceOrder,
  mockCancelOrder,
  mockCreateOrder,
  mockOrderHistory,
  mockStoreOrderQueue,
  mockValidateOrder,
} from "@/api/mock/orderMock";
import type {
  CreateOrderRequest,
  CreateOrderResponse,
  OrderHistoryItem,
  ValidateOrderRequest,
  ValidateOrderResponse,
} from "@/types/order.types";

/** 주문 도메인 API 파사드. 현재는 Mock 구현에 위임한다 (PRD 6.B, 4.2) */
export const orderApi = {
  validate: (request: ValidateOrderRequest): Promise<ValidateOrderResponse> => mockValidateOrder(request),
  create: (request: CreateOrderRequest): Promise<CreateOrderResponse> => mockCreateOrder(request),
  history: (): Promise<OrderHistoryItem[]> => mockOrderHistory(),
  /** 매장 관리자 대시보드(F-05) 전용 */
  storeQueue: (storeId: string): Promise<OrderHistoryItem[]> => mockStoreOrderQueue(storeId),
  advance: (orderId: string): Promise<OrderHistoryItem> => mockAdvanceOrder(orderId),
  cancel: (orderId: string): Promise<OrderHistoryItem> => mockCancelOrder(orderId),
};
