import type { ID } from "@/types/common.types";
import type { PackagingType } from "@/types/cart.types";

export interface OrderLineItemInput {
  menuId: ID;
  quantity: number;
  optionChoiceIds: ID[];
}

export interface ValidateOrderRequest {
  storeId: ID;
  items: OrderLineItemInput[];
}

export interface ValidateOrderIssue {
  menuId: ID;
  menuName: string;
  reason: "SOLD_OUT" | "OPTION_SOLD_OUT" | "PRICE_CHANGED";
  message: string;
}

export interface ValidateOrderResponse {
  isValid: boolean;
  issues: ValidateOrderIssue[];
}

export interface CreateOrderRequest {
  storeId: ID;
  items: OrderLineItemInput[];
  packagingType: PackagingType;
  couponId: ID | null;
  useStamp: boolean;
  /** 클라이언트에서 생성한 멱등성 키. 동일 키로 재요청 시 서버는 같은 주문을 반환해야 한다 */
  idempotencyKey: string;
}

export interface CreateOrderResponse {
  orderId: ID;
  totalAmount: number;
}

export interface ConfirmPaymentRequest {
  orderId: ID;
  paymentKey: string;
  amount: number;
}

export interface ConfirmPaymentResponse {
  orderId: ID;
  approvedAt: string;
}

/** PRD 4.4 주문 상태 전이: PENDING -> ACCEPTED -> PREPARING -> READY -> PICKED_UP (또는 CANCELLED) */
export type OrderStatus = "PENDING" | "ACCEPTED" | "PREPARING" | "READY" | "PICKED_UP" | "CANCELLED";

/** SSE(또는 REST Polling 폴백)로 전달되는 주문 상태 갱신 이벤트 */
export interface OrderTrackingEvent {
  orderId: ID;
  status: OrderStatus;
  updatedAt: string;
  message: string;
}

export interface OrderHistoryItem {
  orderId: ID;
  storeId: ID;
  storeName: string;
  status: OrderStatus;
  totalAmount: number;
  /** 예: "아메리카노 외 1건" */
  itemsSummary: string;
  createdAt: string;
}
