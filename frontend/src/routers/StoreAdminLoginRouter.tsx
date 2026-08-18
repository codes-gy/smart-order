"use client";

import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useStoreLoginForm } from "@/hooks/useStoreLoginForm";

/** 매장 POS/태블릿 로그인 페이지 조립 */
export function StoreAdminLoginRouter() {
  const { form, submit, isLoading } = useStoreLoginForm();

  return (
    <main id="main-content" className="flex min-h-screen flex-col items-center justify-center px-6">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-foreground">Smart Order 매장 관리자</h1>
          <p className="mt-1 text-sm text-foreground/60">매장 코드와 비밀번호로 로그인하세요</p>
        </div>
        <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
          <Input
            label="매장 코드"
            placeholder="STORE001"
            autoComplete="username"
            errorMessage={form.formState.errors.storeCode?.message}
            {...form.register("storeCode")}
          />
          <Input
            label="비밀번호"
            type="password"
            autoComplete="current-password"
            errorMessage={form.formState.errors.password?.message}
            {...form.register("password")}
          />
          <Button type="submit" isLoading={isLoading} loadingText="로그인 중" fullWidth size="lg">
            로그인
          </Button>
        </form>
      </div>
    </main>
  );
}
