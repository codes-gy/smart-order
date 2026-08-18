/**
 * 앱 전역에서 공유하는 상수. localStorage 키는 여기서만 정의해서
 * 여러 레이어(초기 테마 스크립트, zustand persist 등)가 값을 어긋나게
 * 사용하는 문제를 방지한다.
 */

/** themeStore(zustand persist)와 layout.tsx의 FOUC 방지 스크립트가 공유하는 키 */
export const THEME_STORAGE_KEY = "smart-order-theme";

/** cartStore(zustand persist)가 사용할 키 (Phase 5에서 사용) */
export const CART_STORAGE_KEY = "smart-order-cart";

/** authStore(zustand persist)가 사용할 키 (Phase 2에서 사용) */
export const AUTH_STORAGE_KEY = "smart-order-auth";

/** orderTrackingStore(zustand persist)가 사용할 키 (Phase 6에서 사용) */
export const ORDER_TRACKING_STORAGE_KEY = "smart-order-tracking";

/** Mock 단계 API 기본 지연(ms). 실제 API 연동 전까지 네트워크 체감을 재현하기 위함 */
export const MOCK_API_DELAY_MS = 400;

/** PRD 4.1 SSE 재연결 정책: Exponential Backoff 지연 테이블(ms) */
export const SSE_RECONNECT_DELAYS_MS = [1000, 2000, 4000, 8000] as const;

/** PRD 4.1 SSE 재연결 정책: 이 횟수 초과 실패 시 REST Polling으로 폴백 */
export const SSE_MAX_RECONNECT_ATTEMPTS = 3;

/** PRD 4.1 REST Polling 폴백 주기(ms) */
export const POLLING_INTERVAL_MS = 3000;

/** 스탬프 10개 적립 완료 시 적용되는 할인 금액(원) (Mock 규약) */
export const STAMP_REWARD_DISCOUNT = 4500;
