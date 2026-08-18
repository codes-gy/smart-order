package com.gy.smartorder.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class GeoUtilsTest {

    @Test
    fun `같은 좌표는 거리가 0이다`() {
        val distance = GeoUtils.haversineMeters(37.5006, 127.0364, 37.5006, 127.0364)
        assertThat(distance).isEqualTo(0.0, within(0.001))
    }

    @Test
    fun `역삼역과 강남역 사이 거리는 대략 900m 안팎이다`() {
        // 프론트 utils/geo.ts 와 동일한 하버사인 공식으로 계산한 기준값.
        val distance = GeoUtils.haversineMeters(37.5006, 127.0364, 37.4979, 127.0276)
        assertThat(distance).isBetween(700.0, 1_000.0)
    }

    @Test
    fun `거리가 멀수록 큰 값을 반환한다`() {
        val near = GeoUtils.haversineMeters(37.5006, 127.0364, 37.5010, 127.0368)
        val far = GeoUtils.haversineMeters(37.5006, 127.0364, 37.6006, 127.1364)
        assertThat(far).isGreaterThan(near)
    }
}
