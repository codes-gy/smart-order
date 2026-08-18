"use client";

import { useCallback, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api/authApi";
import { useAuthStore } from "@/stores/authStore";
import { ApiError } from "@/types/common.types";

const PHONE_REGEX = /^01[0-9]-?\d{3,4}-?\d{4}$/;

const phoneSchema = z.object({
  phoneNumber: z
    .string()
    .min(1, "휴대폰 번호를 입력해주세요.")
    .regex(PHONE_REGEX, "올바른 휴대폰 번호 형식이 아니에요. (예: 010-1234-5678)"),
});

const otpSchema = z.object({
  code: z
    .string()
    .length(4, "인증번호 4자리를 입력해주세요.")
    .regex(/^\d{4}$/, "숫자 4자리를 입력해주세요."),
});

export type PhoneFormValues = z.infer<typeof phoneSchema>;
export type OtpFormValues = z.infer<typeof otpSchema>;
export type LoginStep = "phone" | "otp";

/**
 * 비회원 주문용 휴대폰 SMS OTP 로그인 플로우 (F-00).
 * 1) phoneForm 제출 → SMS 발송 → step이 "otp"로 전환
 * 2) otpForm 제출 → 인증 성공 시 authStore에 세션 저장
 */
export function useLoginForm() {
  const [step, setStep] = useState<LoginStep>("phone");
  const [phoneNumber, setPhoneNumber] = useState("");
  const setSession = useAuthStore((state) => state.setSession);

  const phoneForm = useForm<PhoneFormValues>({
    resolver: zodResolver(phoneSchema),
    defaultValues: { phoneNumber: "" },
  });

  const otpForm = useForm<OtpFormValues>({
    resolver: zodResolver(otpSchema),
    defaultValues: { code: "" },
  });

  const sendSmsMutation = useMutation({
    mutationFn: (values: PhoneFormValues) => authApi.sendSms({ phoneNumber: values.phoneNumber }),
    onSuccess: (_data, values) => {
      setPhoneNumber(values.phoneNumber);
      setStep("otp");
      otpForm.reset();
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "인증번호 발송에 실패했습니다.";
      phoneForm.setError("phoneNumber", { type: "server", message });
    },
  });

  const verifyOtpMutation = useMutation({
    mutationFn: (values: OtpFormValues) => authApi.verifySms({ phoneNumber, code: values.code }),
    onSuccess: ({ user, tokens }) => {
      setSession(user, tokens);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "인증에 실패했습니다.";
      otpForm.setError("code", { type: "server", message });
    },
  });

  const submitPhone = phoneForm.handleSubmit((values) => sendSmsMutation.mutate(values));
  const submitOtp = otpForm.handleSubmit((values) => verifyOtpMutation.mutate(values));

  const resendOtp = useCallback(() => {
    if (!phoneNumber) return;
    sendSmsMutation.mutate({ phoneNumber });
  }, [phoneNumber, sendSmsMutation]);

  const goBackToPhone = useCallback(() => {
    setStep("phone");
    otpForm.reset();
  }, [otpForm]);

  return {
    step,
    phoneNumber,
    phoneForm,
    otpForm,
    submitPhone,
    submitOtp,
    resendOtp,
    goBackToPhone,
    isSendingSms: sendSmsMutation.isPending,
    isVerifyingOtp: verifyOtpMutation.isPending,
    isLoginComplete: verifyOtpMutation.isSuccess,
  };
}
