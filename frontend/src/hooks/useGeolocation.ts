"use client";

import { useCallback, useEffect, useState } from "react";
import type { StoreLocation } from "@/types/store.types";

export type GeolocationStatus = "loading" | "success" | "error";

export interface UseGeolocationResult {
  coords: StoreLocation;
  status: GeolocationStatus;
  errorMessage: string | null;
  retry: () => void;
}

/** 위치 권한 거부/미지원 시 사용할 폴백 좌표 (역삼역 인근) */
const FALLBACK_COORDS: StoreLocation = { lat: 37.5006, lng: 127.0364 };

function isGeolocationSupported(): boolean {
  return typeof navigator !== "undefined" && "geolocation" in navigator;
}

/**
 * 매장 탐색(F-01)용 현재 위치 훅.
 * 브라우저가 위치 정보를 지원하지 않는 경우는 렌더링 시점에 즉시 판별해 초기 상태에 반영하고,
 * 실제 위치 조회(getCurrentPosition)는 비동기 콜백에서만 상태를 갱신한다.
 */
export function useGeolocation(): UseGeolocationResult {
  const supported = isGeolocationSupported();

  const [coords, setCoords] = useState<StoreLocation>(FALLBACK_COORDS);
  const [status, setStatus] = useState<GeolocationStatus>(supported ? "loading" : "error");
  const [errorMessage, setErrorMessage] = useState<string | null>(
    supported ? null : "이 브라우저는 위치 정보를 지원하지 않아요. 기본 위치로 매장을 보여드릴게요.",
  );
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!supported) return;

    let cancelled = false;

    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (cancelled) return;
        setCoords({ lat: position.coords.latitude, lng: position.coords.longitude });
        setStatus("success");
        setErrorMessage(null);
      },
      (error) => {
        if (cancelled) return;
        setCoords(FALLBACK_COORDS);
        setStatus("error");
        setErrorMessage(
          error.code === error.PERMISSION_DENIED
            ? "위치 접근 권한이 거부되었어요. 기본 위치로 매장을 보여드릴게요."
            : "위치 정보를 가져오지 못했어요. 기본 위치로 매장을 보여드릴게요.",
        );
      },
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 60_000 },
    );

    return () => {
      cancelled = true;
    };
  }, [supported, attempt]);

  const retry = useCallback(() => {
    setStatus("loading");
    setAttempt((n) => n + 1);
  }, []);

  return { coords, status, errorMessage, retry };
}
