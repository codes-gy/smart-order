"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Header } from "@/components/ui/layout/Header";
import { BottomNav } from "@/components/ui/layout/BottomNav";
import { Card } from "@/components/ui/Card";
import { Skeleton, SkeletonText } from "@/components/ui/Skeleton";
import { ErrorAlert } from "@/components/ui/ErrorAlert";
import { Button } from "@/components/ui/Button";
import { RewardsBadge } from "@/components/auth/RewardsBadge";
import { DeleteAccountDialog } from "@/components/auth/DeleteAccountDialog";
import { authApi } from "@/api/authApi";
import { useAuthStore } from "@/stores/authStore";

/** 마이페이지: 프로필/스탬프·쿠폰 현황, 로그아웃, 회원 탈퇴 (F-00) */
export function MyPageRouter() {
  const router = useRouter();
  const clearSession = useAuthStore((state) => state.clearSession);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const { data, isPending, isError, isSuccess, refetch } = useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => authApi.me(),
  });

  const handleLogout = async () => {
    await authApi.logout();
    clearSession();
    router.push("/login");
  };

  return (
    <div className="flex min-h-screen flex-col">
      <Header title="마이페이지" />
      <main id="main-content" className="flex-1 space-y-4 p-4 pb-20">
        {isPending && (
          <Card className="space-y-3">
            <Skeleton className="h-6 w-32" />
            <SkeletonText lines={2} />
          </Card>
        )}

        {isError && <ErrorAlert layout="page" onRetry={() => refetch()} />}

        {isSuccess && data && (
          <Card className="space-y-3">
            <p className="text-lg font-semibold text-foreground">{data.user.name}님</p>
            <RewardsBadge rewards={data.rewards} />
          </Card>
        )}

        <div className="flex flex-col gap-2">
          <Button variant="outline" onClick={handleLogout}>
            로그아웃
          </Button>
          <Button variant="ghost" className="!text-danger" onClick={() => setDeleteDialogOpen(true)}>
            회원 탈퇴
          </Button>
        </div>
      </main>
      <BottomNav />
      <DeleteAccountDialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)} />
    </div>
  );
}
