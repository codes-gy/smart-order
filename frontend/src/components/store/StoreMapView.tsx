"use client";

import Link from "next/link";
import { StoreThumbnail } from "@/components/store/StoreThumbnail";
import type { StoreLocation, StoreSummary } from "@/types/store.types";

const VIEW_RADIUS_METERS = 2000;
const METERS_PER_DEGREE_LAT = 111_320;

function metersPerDegreeLng(latDeg: number): number {
  return METERS_PER_DEGREE_LAT * Math.cos((latDeg * Math.PI) / 180);
}

function projectToPercent(
  origin: StoreLocation,
  target: StoreLocation,
): { xPercent: number; yPercent: number } | null {
  const dxMeters = (target.lng - origin.lng) * metersPerDegreeLng(origin.lat);
  const dyMeters = (target.lat - origin.lat) * METERS_PER_DEGREE_LAT;

  if (Math.abs(dxMeters) > VIEW_RADIUS_METERS || Math.abs(dyMeters) > VIEW_RADIUS_METERS) {
    return null;
  }

  return {
    xPercent: 50 + (dxMeters / VIEW_RADIUS_METERS) * 50,
    yPercent: 50 - (dyMeters / VIEW_RADIUS_METERS) * 50,
  };
}

export interface StoreMapViewProps {
  stores: StoreSummary[];
  userCoords: StoreLocation;
}

/**
 * 실제 지도 SDK(Kakao/Naver/Google Maps 등) 연동 전 단계의 경량 위치 시각화.
 * 사용자 위치를 중심으로 상대 좌표를 계산해 매장 핀을 배치한다.
 * API 키가 필요한 실지도 연동 시 이 컴포넌트 내부 구현만 교체하면 된다.
 */
export function StoreMapView({ stores, userCoords }: StoreMapViewProps) {
  return (
    <div className="relative m-4 h-80 overflow-hidden rounded-2xl border border-border bg-surface-muted">
      <div
        className="absolute left-1/2 top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent ring-4 ring-accent/30"
        aria-hidden="true"
      />
      <span className="absolute left-1/2 top-[calc(50%+10px)] -translate-x-1/2 text-xs text-foreground/60">
        내 위치
      </span>

      {stores.map((store) => {
        const position = projectToPercent(userCoords, store.location);
        if (!position) return null;
        return (
          <Link
            key={store.id}
            href={`/store/${store.id}`}
            className="absolute flex -translate-x-1/2 -translate-y-full flex-col items-center"
            style={{ left: `${position.xPercent}%`, top: `${position.yPercent}%` }}
          >
            <StoreThumbnail name={store.name} className="h-9 w-9 text-xs shadow-md" />
          </Link>
        );
      })}
    </div>
  );
}
