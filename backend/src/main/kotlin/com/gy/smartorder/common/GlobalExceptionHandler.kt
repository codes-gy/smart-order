package com.gy.smartorder.common

import com.gy.smartorder.common.exception.ApiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * 도메인 예외를 프론트가 기대하는 공통 에러 바디(`ErrorResponse`)로 변환하는 전역 핸들러.
 * 이게 없으면 "존재하지 않는 매장 id" 같은 흔한 케이스도 500으로 새어나가고,
 * 프론트 apiClient의 `ApiError` 매핑이 깨진다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(ex.status)
            .body(ErrorResponse(ex.code, ex.message ?: "요청을 처리할 수 없습니다.", ex.details))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors
            .groupBy({ it.field }) { it.defaultMessage ?: "유효하지 않은 값입니다." }
        return ResponseEntity.badRequest()
            .body(ErrorResponse("VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", details))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(ErrorResponse("INVALID_PARAMETER", "'${ex.name}' 파라미터 형식이 올바르지 않습니다."))

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(ErrorResponse("MISSING_PARAMETER", "'${ex.parameterName}' 파라미터가 필요합니다."))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(ErrorResponse("INVALID_REQUEST_BODY", "요청 본문을 해석할 수 없습니다."))

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "일시적인 오류가 발생했습니다."))
}
