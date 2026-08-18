package com.gy.smartorder.common.exception

import org.springframework.http.HttpStatus

/**
 * 도메인 전역에서 쓰는 공통 API 예외.
 * `GlobalExceptionHandler`가 이 타입을 잡아 프론트 `ApiErrorBody`({ code, message }) 규격으로 변환한다.
 */
open class ApiException(
    val status: HttpStatus,
    val code: String,
    message: String,
) : RuntimeException(message)

class NotFoundException(code: String, message: String) : ApiException(HttpStatus.NOT_FOUND, code, message)

class BadRequestException(code: String, message: String) : ApiException(HttpStatus.BAD_REQUEST, code, message)
