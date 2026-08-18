package com.gy.smartorder.common

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 두 좌표 간 직선 거리를 하버사인 공식으로 계산한다.
 * 프론트엔드 mock(`src/utils/geo.ts`)과 동일한 지구 반지름 상수·공식을 사용해
 * 실제 API로 전환돼도 거리 계산 결과가 어긋나지 않도록 맞춘다.
 */
object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val sinDLat = sin(dLat / 2)
        val sinDLng = sin(dLng / 2)
        val h = sinDLat * sinDLat + cos(radLat1) * cos(radLat2) * sinDLng * sinDLng

        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(h), sqrt(1 - h))
    }
}
