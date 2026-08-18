"use client";

import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";

export interface GeofenceWarningModalProps {
  open: boolean;
  storeName: string;
  distanceKm: string;
  onConfirm: () => void;
  onCancel: () => void;
}

/** 지점 착오 방지: 사용자 위치와 매장 거리가 1km 이상일 때 노출되는 확인 모달 (F-01) */
export function GeofenceWarningModal({
  open,
  storeName,
  distanceKm,
  onConfirm,
  onCancel,
}: GeofenceWarningModalProps) {
  return (
    <Modal
      open={open}
      onClose={onCancel}
      title="매장을 다시 확인해주세요"
      description={`현재 위치와 매장 거리가 ${distanceKm}km 떨어져 있어요. [${storeName}]이(가) 맞으신가요?`}
      footer={
        <>
          <Button variant="outline" onClick={onCancel}>
            다른 매장 찾기
          </Button>
          <Button variant="primary" onClick={onConfirm}>
            네, 맞아요
          </Button>
        </>
      }
    />
  );
}
