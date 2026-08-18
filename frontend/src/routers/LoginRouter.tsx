"use client";

import { Header } from "@/components/ui/layout/Header";
import { SocialLoginButtons } from "@/components/auth/SocialLoginButtons";
import { PhoneOtpForm } from "@/components/auth/PhoneOtpForm";

/** 고객용 로그인 페이지 조립 (소셜 로그인 + 비회원 SMS 인증) */
export function LoginRouter() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header title="로그인" />
      <main id="main-content" className="flex flex-1 flex-col justify-center gap-8 px-6 py-10">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Smart Order</h1>
          <p className="mt-1 text-sm text-foreground/60">출근길, 기다리지 않는 커피 한 잔</p>
        </div>
        <SocialLoginButtons />
        <div className="flex items-center gap-3 text-xs text-foreground/70">
          <span className="h-px flex-1 bg-border" aria-hidden="true" />
          또는 휴대폰 번호로 계속하기
          <span className="h-px flex-1 bg-border" aria-hidden="true" />
        </div>
        <PhoneOtpForm />
      </main>
    </div>
  );
}
