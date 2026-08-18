import {
  mockCoupons,
  mockDeleteAccount,
  mockFavorites,
  mockLogout,
  mockMe,
  mockRefresh,
  mockRewards,
  mockSmsSend,
  mockSmsVerify,
  mockSocialLogin,
  mockStoreLogin,
} from "@/api/mock/authMock";
import type {
  AuthTokens,
  AuthUser,
  Coupon,
  FavoriteStore,
  MeResponse,
  RewardsSummary,
  SmsSendRequest,
  SmsVerifyRequest,
  SocialLoginRequest,
  StoreLoginRequest,
  StoreSession,
} from "@/types/auth.types";

/**
 * 인증 도메인 API 파사드.
 * 현재는 Mock 구현(src/api/mock/authMock.ts)에 위임한다.
 * 실제 백엔드 연동 시 각 함수 본문만 apiClient의 apiRequest(...) 호출로 교체하면 되도록
 * 요청/응답 타입을 PRD 6장 API 명세에 맞춰 최종 형태로 정의해두었다.
 */
export const authApi = {
  socialLogin: (request: SocialLoginRequest): Promise<{ user: AuthUser; tokens: AuthTokens }> =>
    mockSocialLogin(request),

  sendSms: (request: SmsSendRequest): Promise<{ success: true }> => mockSmsSend(request),

  verifySms: (request: SmsVerifyRequest): Promise<{ user: AuthUser; tokens: AuthTokens }> =>
    mockSmsVerify(request),

  refresh: (): Promise<{ accessToken: string }> => mockRefresh(),

  logout: (): Promise<void> => mockLogout(),

  storeLogin: (request: StoreLoginRequest): Promise<StoreSession> => mockStoreLogin(request),

  me: (): Promise<MeResponse> => mockMe(),

  rewards: (): Promise<RewardsSummary> => mockRewards(),

  favorites: (): Promise<FavoriteStore[]> => mockFavorites(),

  deleteAccount: (): Promise<void> => mockDeleteAccount(),

  coupons: (): Promise<Coupon[]> => mockCoupons(),
};
