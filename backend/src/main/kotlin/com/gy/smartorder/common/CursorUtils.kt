package com.gy.smartorder.common

import java.util.Base64

/**
 * 커서 기반 페이징(PRD 4.3)용 오프셋 커서 인코더/디코더.
 * 프론트 mock(`storeMock.ts`)의 `btoa(JSON.stringify({ offset }))` 방식과
 * 동일한 포맷(offset을 담은 JSON을 Base64로 감싼 문자열)을 사용한다.
 */
object CursorUtils {
    private val OFFSET_REGEX = Regex(""""offset"\s*:\s*(\d+)""")

    fun encode(offset: Int): String {
        val json = "{\"offset\":$offset}"
        return Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
    }

    /** 파싱할 수 없거나 비어 있는 커서는 첫 페이지(0)로 취급한다. */
    fun decodeOffset(cursor: String?): Int {
        if (cursor.isNullOrBlank()) return 0
        return runCatching {
            val json = String(Base64.getDecoder().decode(cursor), Charsets.UTF_8)
            OFFSET_REGEX.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }.getOrDefault(0)
    }
}
