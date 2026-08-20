package com.gy.smartorder.common.exception

import org.springframework.http.HttpStatus

/**
 * 도메인 전역에서 쓰는 공통 API 예외.
 * `GlobalExceptionHandler`가 이 타입을 잡아 프론트 `ApiErrorBody`({ code, message, details? }) 규격으로 변환한다.
 */
open class ApiException(
    val status: HttpStatus,
    val code: String,
    message: String,
    val details: Map<String, List<String>>? = null,
) : RuntimeException(message)

class NotFoundException(code: String, message: String) : ApiException(HttpStatus.NOT_FOUND, code, message)

class BadRequestException(code: String, message: String) : ApiException(HttpStatus.BAD_REQUEST, code, message)

/** 요청 자체는 유효하지만 현재 서버 상태와 충돌하는 경우 (예: 주문 검증 실패, 품절 등). */
class ConflictException(
    code: String,
    message: String,
    details: Map<String, List<String>>? = null,
) : ApiException(HttpStatus.CONFLICT, code, message, details)
