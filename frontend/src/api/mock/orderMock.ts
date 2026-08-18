import { mockFetchStoreMenu } from "@/api/mock/menuMock";
import { mockFetchStoreDetail } from "@/api/mock/storeMock";
import { getCouponDiscountAmount, STAMP_REWARD_DISCOUNT } from "@/api/mock/rewardsMock";
import { peekOrderStatus, registerOrder, setOrderStatus } from "@/api/mock/orderTrackingMock";
import { ApiError } from "@/types/common.types";
import { MOCK_API_DELAY_MS } from "@/utils/constants";
import type { MenuItem } from "@/types/menu.types";
import type {
  ConfirmPaymentRequest,
  ConfirmPaymentResponse,
  CreateOrderRequest,
  CreateOrderResponse,
  OrderHistoryItem,
  OrderLineItemInput,
  ValidateOrderIssue,
  ValidateOrderRequest,
  ValidateOrderResponse,
} from "@/types/order.types";

function delay(ms: number = MOCK_API_DELAY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** X-Idempotency-Key -> 이전 응답. 동일 키로 재요청하면 새 주문을 만들지 않고 캐시된 결과를 그대로 반환한다 */
const orderIdempotencyCache = new Map<string, CreateOrderResponse>();

const ORDER_HISTORY: OrderHistoryItem[] = [];

/** 데모용 초기 주문 내역 2건 (하나는 완료, 하나는 취소) — 진행 타이머는 시작하지 않고 상태를 고정한다 */
function seedOrderHistory(): void {
  const seeds: OrderHistoryItem[] = [
    {
      orderId: "order-seed-1",
      storeId: "store-1",
      storeName: "스마트오더 역삼역점",
      status: "PICKED_UP",
      totalAmount: 9500,
      itemsSummary: "아메리카노 외 1건",
      createdAt: new Date(Date.now() - 2 * 86_400_000).toISOString(),
    },
    {
      orderId: "order-seed-2",
      storeId: "store-2",
      storeName: "스마트오더 강남역점",
      status: "CANCELLED",
      totalAmount: 4500,
      itemsSummary: "카페라떼",
      createdAt: new Date(Date.now() - 6 * 86_400_000).toISOString(),
    },
  ];
  for (const seed of seeds) {
    registerOrder(seed.orderId, seed.status);
    ORDER_HISTORY.push(seed);
  }
}
seedOrderHistory();

function buildItemsSummary(menuItems: MenuItem[], items: OrderLineItemInput[]): string {
  if (items.length === 0) return "";
  const firstName = menuItems.find((candidate) => candidate.id === items[0].menuId)?.name ?? "메뉴";
  return items.length > 1 ? `${firstName} 외 ${items.length - 1}건` : firstName;
}

function findLineIssue(item: MenuItem, line: OrderLineItemInput): ValidateOrderIssue | null {
  if (item.isSoldOut) {
    return { menuId: item.id, menuName: item.name, reason: "SOLD_OUT", message: `${item.name}이(가) 품절되었어요.` };
  }
  const soldOutChoice = item.optionGroups
    .flatMap((group) => group.choices)
    .find((choice) => line.optionChoiceIds.includes(choice.id) && choice.isSoldOut);
  if (soldOutChoice) {
    return {
      menuId: item.id,
      menuName: item.name,
      reason: "OPTION_SOLD_OUT",
      message: `${item.name}의 '${soldOutChoice.label}' 옵션이 품절되었어요.`,
    };
  }
  return null;
}

async function checkIssues(request: ValidateOrderRequest | CreateOrderRequest): Promise<ValidateOrderIssue[]> {
  const menu = await mockFetchStoreMenu(request.storeId);
  const issues: ValidateOrderIssue[] = [];

  for (const line of request.items) {
    const item = menu.items.find((candidate) => candidate.id === line.menuId);
    if (!item) {
      issues.push({
        menuId: line.menuId,
        menuName: line.menuId,
        reason: "SOLD_OUT",
        message: "더 이상 판매하지 않는 메뉴예요.",
      });
      continue;
    }
    const issue = findLineIssue(item, line);
    if (issue) issues.push(issue);
  }

  return issues;
}

function calculateItemsAmount(menuItems: MenuItem[], items: OrderLineItemInput[]): number {
  return items.reduce((sum, line) => {
    const item = menuItems.find((candidate) => candidate.id === line.menuId);
    if (!item) return sum;
    const optionsPrice = item.optionGroups.reduce((groupSum, group) => {
      const choicesPrice = group.choices
        .filter((choice) => line.optionChoiceIds.includes(choice.id))
        .reduce((choiceSum, choice) => choiceSum + choice.priceDelta, 0);
      return groupSum + choicesPrice;
    }, 0);
    return sum + (item.basePrice + optionsPrice) * line.quantity;
  }, 0);
}

/** 주문 생성 직전 재고/가격 변동을 서버 기준으로 재검증한다 (PRD 3.3, F-03) */
export async function mockValidateOrder(request: ValidateOrderRequest): Promise<ValidateOrderResponse> {
  await delay();
  const issues = await checkIssues(request);
  return { isValid: issues.length === 0, issues };
}

/**
 * 주문을 생성한다. 클라이언트가 보낸 X-Idempotency-Key(request.idempotencyKey)에 해당하는
 * 결과가 이미 있으면 새로 계산하지 않고 그대로 재반환해 진짜 멱등 동작을 재현한다.
 */
export async function mockCreateOrder(request: CreateOrderRequest): Promise<CreateOrderResponse> {
  const cached = orderIdempotencyCache.get(request.idempotencyKey);
  if (cached) {
    await delay(150);
    return cached;
  }

  await delay();

  const issues = await checkIssues(request);
  if (issues.length > 0) {
    throw new ApiError(409, {
      code: "ORDER_VALIDATION_FAILED",
      message: "주문 내용이 변경되었어요. 장바구니를 다시 확인해주세요.",
      details: { issues: issues.map((issue) => issue.message) },
    });
  }

  const menu = await mockFetchStoreMenu(request.storeId);
  const itemsAmount = calculateItemsAmount(menu.items, request.items);
  const couponDiscount = getCouponDiscountAmount(request.couponId);
  const stampDiscount = request.useStamp ? STAMP_REWARD_DISCOUNT : 0;
  const totalAmount = Math.max(0, itemsAmount - couponDiscount - stampDiscount);

  const response: CreateOrderResponse = {
    orderId: `order-${crypto.randomUUID()}`,
    totalAmount,
  };
  orderIdempotencyCache.set(request.idempotencyKey, response);

  const store = await mockFetchStoreDetail(request.storeId).catch(() => null);
  ORDER_HISTORY.unshift({
    orderId: response.orderId,
    storeId: request.storeId,
    storeName: store?.name ?? "매장",
    status: "PENDING",
    totalAmount,
    itemsSummary: buildItemsSummary(menu.items, request.items),
    createdAt: new Date().toISOString(),
  });
  // 결제 확인(F-04) 이후 실시간 추적이 가능하도록 이 시점부터 상태 진행 타이머를 시작한다.
  registerOrder(response.orderId, "PENDING");

  return response;
}

/** 주문 내역(F-04 연계) 목록. 각 항목의 상태는 추적 레지스트리의 최신 값으로 갱신해 반환한다 */
export async function mockOrderHistory(): Promise<OrderHistoryItem[]> {
  await delay();
  return ORDER_HISTORY.map((entry) => ({
    ...entry,
    status: peekOrderStatus(entry.orderId),
  }));
}

export async function mockConfirmPayment(request: ConfirmPaymentRequest): Promise<ConfirmPaymentResponse> {
  await delay();
  return { orderId: request.orderId, approvedAt: new Date().toISOString() };
}

/** 매장 관리자 대시보드(F-05)의 주문 큐. 진행 중/완료 여부와 무관하게 해당 매장의 전체 내역을 반환한다 */
export async function mockStoreOrderQueue(storeId: string): Promise<OrderHistoryItem[]> {
  await delay(300);
  return ORDER_HISTORY.filter((entry) => entry.storeId === storeId).map((entry) => ({
    ...entry,
    status: peekOrderStatus(entry.orderId),
  }));
}

const NEXT_STATUS: Partial<Record<OrderHistoryItem["status"], OrderHistoryItem["status"]>> = {
  PENDING: "ACCEPTED",
  ACCEPTED: "PREPARING",
  PREPARING: "READY",
  READY: "PICKED_UP",
};

function findHistoryEntryOrThrow(orderId: string): OrderHistoryItem {
  const entry = ORDER_HISTORY.find((order) => order.orderId === orderId);
  if (!entry) {
    throw new ApiError(404, { code: "ORDER_NOT_FOUND", message: "주문을 찾을 수 없어요." });
  }
  return entry;
}

/** 매장 관리자가 다음 단계로 진행시킨다 (접수 -> 제조 중 -> 픽업 준비 -> 픽업 완료). F-05 */
export async function mockAdvanceOrder(orderId: string): Promise<OrderHistoryItem> {
  await delay(200);
  const entry = findHistoryEntryOrThrow(orderId);
  const current = peekOrderStatus(orderId);
  const next = NEXT_STATUS[current];
  if (!next) {
    throw new ApiError(409, { code: "INVALID_TRANSITION", message: "더 이상 진행할 수 없는 주문이에요." });
  }
  setOrderStatus(orderId, next);
  return { ...entry, status: next };
}

/** 매장 관리자가 주문을 거절/취소한다. F-05 */
export async function mockCancelOrder(orderId: string): Promise<OrderHistoryItem> {
  await delay(200);
  const entry = findHistoryEntryOrThrow(orderId);
  setOrderStatus(orderId, "CANCELLED");
  return { ...entry, status: "CANCELLED" };
}
