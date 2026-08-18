"use client";

import { useEffect, useState } from "react";
import { Controller } from "react-hook-form";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { OtpInputField } from "@/components/auth/OtpInputField";
import { useLoginForm, type LoginStep } from "@/hooks/useLoginForm";

const RESEND_COOLDOWN_SECONDS = 60;

/** 비회원 주문용 휴대폰 SMS OTP 2단계 폼 (F-00) */
export function PhoneOtpForm() {
  const {
    step,
    phoneNumber,
    phoneForm,
    otpForm,
    submitPhone,
    submitOtp,
    resendOtp,
    goBackToPhone,
    isSendingSms,
    isVerifyingOtp,
  } = useLoginForm();

  const [cooldown, setCooldown] = useState(0);
  // step이 "otp"로 바뀐 시점을 렌더링 중에 감지해 쿨다운을 초기화한다.
  // (useEffect 안에서 동기적으로 setState 하는 대신, React가 권장하는
  // "이전 값과 비교해 렌더링 중 상태를 조정하는" 패턴을 사용)
  const [trackedStep, setTrackedStep] = useState<LoginStep>(step);
  if (step !== trackedStep) {
    setTrackedStep(step);
    if (step === "otp") setCooldown(RESEND_COOLDOWN_SECONDS);
  }

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown((seconds) => seconds - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const handleResend = () => {
    resendOtp();
    setCooldown(RESEND_COOLDOWN_SECONDS);
  };

  if (step === "phone") {
    return (
      <form onSubmit={submitPhone} className="flex flex-col gap-4" noValidate>
        <Input
          label="휴대폰 번호"
          placeholder="010-1234-5678"
          inputMode="tel"
          autoComplete="tel"
          errorMessage={phoneForm.formState.errors.phoneNumber?.message}
          {...phoneForm.register("phoneNumber")}
        />
        <Button type="submit" isLoading={isSendingSms} loadingText="발송 중" fullWidth>
          인증번호 받기
        </Button>
      </form>
    );
  }

  return (
    <form onSubmit={submitOtp} className="flex flex-col gap-4" noValidate>
      <p className="text-center text-sm text-foreground/70">
        <strong className="text-foreground">{phoneNumber}</strong>로 전송된 인증번호 4자리를 입력해주세요.
      </p>
      <Controller
        control={otpForm.control}
        name="code"
        render={({ field }) => (
          <OtpInputField
            value={field.value}
            onChange={field.onChange}
            error={Boolean(otpForm.formState.errors.code)}
            disabled={isVerifyingOtp}
            autoFocus
          />
        )}
      />
      {otpForm.formState.errors.code && (
        <p role="alert" className="text-center text-sm text-danger">
          {otpForm.formState.errors.code.message}
        </p>
      )}
      <Button type="submit" isLoading={isVerifyingOtp} loadingText="확인 중" fullWidth>
        인증 완료
      </Button>
      <div className="flex items-center justify-center gap-4 text-sm">
        <button type="button" onClick={goBackToPhone} className="text-foreground/60 underline">
          번호 다시 입력
        </button>
        <button
          type="button"
          onClick={handleResend}
          disabled={cooldown > 0}
          className="text-accent-text underline disabled:text-foreground/30"
        >
          {cooldown > 0 ? `재전송 (${cooldown}s)` : "인증번호 재전송"}
        </button>
      </div>
    </form>
  );
}
