import { create } from "zustand";
import { persist } from "zustand/middleware";
import { AUTH_STORAGE_KEY } from "@/utils/constants";
import type { AuthTokens, AuthUser, StoreSession } from "@/types/auth.types";

interface AuthState {
  user: AuthUser | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  storeSession: StoreSession | null;
  isStoreAuthenticated: boolean;
  setSession: (user: AuthUser, tokens: AuthTokens) => void;
  setAccessToken: (accessToken: string) => void;
  clearSession: () => void;
  setStoreSession: (session: StoreSession) => void;
  clearStoreSession: () => void;
}

/**
 * 고객 세션과 매장(POS) 세션을 함께 보관한다. 두 세션은 서로 다른 스코프의 토큰이며
 * 실제 기기에서는 대개 둘 중 하나만 사용되지만, 상태 저장 구조를 분리해두면
 * 매장 태블릿에서 고객 데모를 함께 띄우는 경우에도 안전하다.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      tokens: null,
      isAuthenticated: false,
      storeSession: null,
      isStoreAuthenticated: false,

      setSession: (user, tokens) => set({ user, tokens, isAuthenticated: true }),

      setAccessToken: (accessToken) =>
        set((state) => ({
          tokens: state.tokens
            ? { ...state.tokens, accessToken }
            : { accessToken, refreshToken: "" },
        })),

      clearSession: () => set({ user: null, tokens: null, isAuthenticated: false }),

      setStoreSession: (storeSession) => set({ storeSession, isStoreAuthenticated: true }),

      clearStoreSession: () => set({ storeSession: null, isStoreAuthenticated: false }),
    }),
    {
      name: AUTH_STORAGE_KEY,
      partialize: (state) => ({
        user: state.user,
        tokens: state.tokens,
        isAuthenticated: state.isAuthenticated,
        storeSession: state.storeSession,
        isStoreAuthenticated: state.isStoreAuthenticated,
      }),
    },
  ),
);
