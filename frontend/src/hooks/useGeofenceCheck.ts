"use client";

import { useMemo } from "react";
import { haversineDistanceMeters } from "@/utils/geo";
import type { StoreLocation } from "@/types/store.types";

const GEOFENCE_THRESHOLD_METERS = 1000;

export interface GeofenceCheckResult {
  distanceMeters: number;
  distanceKm: string;
  /** 사용자-매장 거리가 1km 이상이면 true (PRD F-01 지점 착오 방지 경고 기준) */
  needsWarning: boolean;
}

export function useGeofenceCheck(
  userCoords: StoreLocation,
  storeCoords: StoreLocation,
): GeofenceCheckResult {
  return useMemo(() => {
    const distanceMeters = haversineDistanceMeters(userCoords, storeCoords);
    return {
      distanceMeters,
      distanceKm: (distanceMeters / 1000).toFixed(1),
      needsWarning: distanceMeters >= GEOFENCE_THRESHOLD_METERS,
    };
  }, [userCoords, storeCoords]);
}
