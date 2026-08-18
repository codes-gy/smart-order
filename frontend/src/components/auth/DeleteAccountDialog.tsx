"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { authApi } from "@/api/authApi";
import { useAuthStore } from "@/stores/authStore";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/types/common.types";

export interface DeleteAccountDialogProps {
  open: boolean;
  onClose: () => void;
}

/** 회원 탈퇴(계정 삭제) 확인 다이얼로그. 앱스토어 심사 대응용 필수 기능 (F-00) */
export function DeleteAccountDialog({ open, onClose }: DeleteAccountDialogProps) {
  const router = useRouter();
  const clearSession = useAuthStore((state) => state.clearSession);
  const { show } = useToast();

  const mutation = useMutation({
    mutationFn: () => authApi.deleteAccount(),
    onSuccess: () => {
      clearSession();
      onClose();
      show({ title: "회원 탈퇴가 완료되었어요", variant: "success" });
      router.push("/login");
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "탈퇴 처리에 실패했습니다.";
      show({ title: "탈퇴 실패", description: message, variant: "error" });
    },
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="정말 탈퇴하시겠어요?"
      description="탈퇴 시 보유하신 스탬프와 쿠폰이 모두 사라지며 복구할 수 없어요."
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>
            취소
          </Button>
          <Button
            variant="danger"
            onClick={() => mutation.mutate()}
            isLoading={mutation.isPending}
            loadingText="처리 중"
          >
            탈퇴하기
          </Button>
        </>
      }
    />
  );
}
