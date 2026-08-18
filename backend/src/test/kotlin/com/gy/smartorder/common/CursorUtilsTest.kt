package com.gy.smartorder.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CursorUtilsTest {

    @Test
    fun `인코딩한 커서를 디코딩하면 원래 offset을 돌려준다`() {
        val cursor = CursorUtils.encode(30)
        assertThat(CursorUtils.decodeOffset(cursor)).isEqualTo(30)
    }

    @Test
    fun `커서가 null이면 첫 페이지(0)로 취급한다`() {
        assertThat(CursorUtils.decodeOffset(null)).isEqualTo(0)
    }

    @Test
    fun `커서가 빈 문자열이면 첫 페이지(0)로 취급한다`() {
        assertThat(CursorUtils.decodeOffset("")).isEqualTo(0)
    }

    @Test
    fun `깨진 커서는 예외를 던지지 않고 0으로 취급한다`() {
        assertThat(CursorUtils.decodeOffset("not-a-valid-cursor!!")).isEqualTo(0)
    }
}
