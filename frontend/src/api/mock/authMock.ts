import { ApiError } from "@/types/common.types";
import { MOCK_API_DELAY_MS } from "@/utils/constants";
import { MOCK_COUPONS } from "@/api/mock/rewardsMock";
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

function delay(ms: number = MOCK_API_DELAY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function createMockTokens(): AuthTokens {
  return {
    accessToken: `mock-access-${crypto.randomUUID()}`,
    refreshToken: `mock-refresh-${crypto.randomUUID()}`,
  };
}

const MOCK_MEMBER_USER: AuthUser = {
  id: "user-1",
  name: "김스마트",
  phoneNumber: "010-1234-5678",
  isGuest: false,
};

const MOCK_REWARDS: RewardsSummary = {
  stampCount: 8,
  stampGoal: 10,
  availableCouponCount: 2,
};

const MOCK_FAVORITES: FavoriteStore[] = [
  { storeId: "store-1", storeName: "스마트오더 역삼역점", lastOrderedAt: new Date().toISOString() },
];

const PHONE_REGEX = /^01[0-9]-?\d{3,4}-?\d{4}$/;

export async function mockSocialLogin(
  request: SocialLoginRequest,
): Promise<{ user: AuthUser; tokens: AuthTokens }> {
  await delay();
  return {
    user: { ...MOCK_MEMBER_USER, id: `${request.provider}-${MOCK_MEMBER_USER.id}` },
    tokens: createMockTokens(),
  };
}

export async function mockSmsSend(request: SmsSendRequest): Promise<{ success: true }> {
  await delay();
  if (!PHONE_REGEX.test(request.phoneNumber)) {
    throw new ApiError(400, { code: "INVALID_PHONE", message: "휴대폰 번호 형식을 확인해주세요." });
  }
  return { success: true };
}

export async function mockSmsVerify(
  request: SmsVerifyRequest,
): Promise<{ user: AuthUser; tokens: AuthTokens }> {
  await delay();
  // Mock 단계 규약: 인증번호 1234만 성공 처리
  if (request.code !== "1234") {
    throw new ApiError(400, { code: "INVALID_OTP", message: "인증번호가 올바르지 않아요. 다시 확인해주세요." });
  }
  return {
    user: {
      id: `guest-${request.phoneNumber}`,
      name: "게스트",
      phoneNumber: request.phoneNumber,
      isGuest: true,
    },
    tokens: createMockTokens(),
  };
}

export async function mockRefresh(): Promise<{ accessToken: string }> {
  await delay(150);
  return { accessToken: `mock-access-${crypto.randomUUID()}` };
}

export async function mockLogout(): Promise<void> {
  await delay(150);
}

export async function mockStoreLogin(request: StoreLoginRequest): Promise<StoreSession> {
  await delay();
  // Mock 단계 데모 계정: STORE001 / password123
  if (request.storeCode !== "STORE001" || request.password !== "password123") {
    throw new ApiError(401, {
      code: "INVALID_CREDENTIALS",
      message: "매장 코드 또는 비밀번호가 올바르지 않아요.",
    });
  }
  return {
    storeId: "store-1",
    storeName: "스마트오더 역삼역점",
    accessToken: `mock-store-access-${crypto.randomUUID()}`,
  };
}

export async function mockMe(): Promise<MeResponse> {
  await delay();
  return { user: MOCK_MEMBER_USER, rewards: MOCK_REWARDS };
}

export async function mockRewards(): Promise<RewardsSummary> {
  await delay();
  return MOCK_REWARDS;
}

export async function mockFavorites(): Promise<FavoriteStore[]> {
  await delay();
  return MOCK_FAVORITES;
}

export async function mockDeleteAccount(): Promise<void> {
  await delay();
}

export async function mockCoupons(): Promise<Coupon[]> {
  await delay();
  return MOCK_COUPONS;
}
