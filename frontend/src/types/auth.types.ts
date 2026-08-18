import type { ID } from "@/types/common.types";

export type SocialProvider = "kakao" | "apple";

export interface SocialLoginRequest {
  provider: SocialProvider;
  accessToken: string;
}

export interface SmsSendRequest {
  phoneNumber: string;
}

export interface SmsVerifyRequest {
  phoneNumber: string;
  code: string;
}

export interface StoreLoginRequest {
  storeCode: string;
  password: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface AuthUser {
  id: ID;
  name: string;
  phoneNumber: string | null;
  isGuest: boolean;
}

/** 매장 POS/태블릿 로그인 세션 (고객 세션과는 별개의 스코프) */
export interface StoreSession {
  storeId: ID;
  storeName: string;
  accessToken: string;
}

export interface RewardsSummary {
  stampCount: number;
  stampGoal: number;
  availableCouponCount: number;
}

export interface FavoriteStore {
  storeId: ID;
  storeName: string;
  lastOrderedAt: string;
}

export interface MeResponse {
  user: AuthUser;
  rewards: RewardsSummary;
}

export interface Coupon {
  id: ID;
  name: string;
  discountAmount: number;
  expiresAt: string;
}
