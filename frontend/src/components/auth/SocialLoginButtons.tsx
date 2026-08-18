"use client";

import { Button } from "@/components/ui/Button";
import { useSocialLogin } from "@/hooks/useSocialLogin";
import type { SocialProvider } from "@/types/auth.types";

const PROVIDERS: { id: SocialProvider; label: string; className: string }[] = [
  {
    id: "kakao",
    label: "카카오로 시작하기",
    className: "bg-[#FEE500] text-[#191600] hover:opacity-90",
  },
  {
    id: "apple",
    label: "Apple로 시작하기",
    className: "bg-black text-white hover:opacity-90 dark:bg-white dark:text-black",
  },
];

/** 소셜 간편 로그인 버튼 그룹 (F-00) */
export function SocialLoginButtons() {
  const { loginWith, isLoading, pendingProvider, errorMessage } = useSocialLogin();

  return (
    <div className="flex flex-col gap-3">
      {PROVIDERS.map((provider) => (
        <Button
          key={provider.id}
          type="button"
          variant="unstyled"
          fullWidth
          className={provider.className}
          isLoading={isLoading && pendingProvider === provider.id}
          loadingText="로그인 중"
          disabled={isLoading}
          onClick={() => loginWith(provider.id)}
        >
          {provider.label}
        </Button>
      ))}
      {errorMessage && (
        <p role="alert" className="text-center text-sm text-danger">
          {errorMessage}
        </p>
      )}
    </div>
  );
}
