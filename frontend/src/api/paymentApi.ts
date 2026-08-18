import { mockConfirmPayment } from "@/api/mock/orderMock";
import type { ConfirmPaymentRequest, ConfirmPaymentResponse } from "@/types/order.types";

/** 결제 도메인 API 파사드. 현재는 Mock 구현에 위임한다 (PRD 6.B, 4.2) */
export const paymentApi = {
  confirm: (request: ConfirmPaymentRequest): Promise<ConfirmPaymentResponse> => mockConfirmPayment(request),
};
