"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { authApi } from "@/api/authApi";
import { useAuthStore } from "@/stores/authStore";
import { ApiError } from "@/types/common.types";
import type { SocialProvider } from "@/types/auth.types";

/** Kakao/Apple 소셜 로그인 버튼용 훅. 실제 SDK 연동 전까지는 mock accessToken을 전달한다 */
export function useSocialLogin() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);

  const mutation = useMutation({
    mutationFn: (provider: SocialProvider) =>
      authApi.socialLogin({ provider, accessToken: `mock-oauth-token-${provider}` }),
    onSuccess: ({ user, tokens }) => {
      setSession(user, tokens);
      router.push("/");
    },
  });

  const errorMessage =
    mutation.error instanceof ApiError ? mutation.error.message : mutation.error ? "로그인에 실패했습니다." : null;

  return {
    loginWith: mutation.mutate,
    isLoading: mutation.isPending,
    pendingProvider: mutation.variables,
    errorMessage,
  };
}
