package com.gy.smartorder.common

/** 프론트 `ApiErrorBody`(`{ code, message, details? }`)와 1:1로 대응하는 에러 응답 바디. */
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, List<String>>? = null,
)
