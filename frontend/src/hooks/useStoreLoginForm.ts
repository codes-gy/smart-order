"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { authApi } from "@/api/authApi";
import { useAuthStore } from "@/stores/authStore";
import { ApiError } from "@/types/common.types";

const storeLoginSchema = z.object({
  storeCode: z.string().min(1, "매장 코드를 입력해주세요."),
  password: z.string().min(8, "비밀번호는 8자 이상이어야 해요."),
});

export type StoreLoginFormValues = z.infer<typeof storeLoginSchema>;

/** 매장 POS/태블릿 로그인 폼 (F-00, /auth/store-login) */
export function useStoreLoginForm() {
  const router = useRouter();
  const setStoreSession = useAuthStore((state) => state.setStoreSession);

  const form = useForm<StoreLoginFormValues>({
    resolver: zodResolver(storeLoginSchema),
    defaultValues: { storeCode: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: (values: StoreLoginFormValues) => authApi.storeLogin(values),
    onSuccess: (session) => {
      setStoreSession(session);
      router.push("/store-admin/dashboard");
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "로그인에 실패했습니다.";
      form.setError("password", { type: "server", message });
    },
  });

  return {
    form,
    submit: form.handleSubmit((values) => mutation.mutate(values)),
    isLoading: mutation.isPending,
  };
}
