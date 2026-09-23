package com.gy.smartorder.config.passport

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * 인증은 됐지만(로그인 상태) 권한이 없는 요청(예: hasRole 불일치)에 대해
 * `ErrorResponse`(`{ code, message }`) 규격으로 403을 내려준다.
 *
 * 이게 없으면 Spring Security가 기본 `AccessDeniedHandlerImpl`로 처리해 Whitelabel/기본
 * 에러 바디를 반환하고, 프론트 `ApiErrorBody` 파싱이 깨진다 (GlobalExceptionHandler는
 * MVC 컨트롤러 진입 이후만 잡으므로, 필터 단계에서 걸러지는 이 예외는 잡지 못한다).
 */
@Component
class JwtAccessDeniedHandler : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            """{"code":"FORBIDDEN","message":"접근 권한이 없습니다."}"""
        )
    }
}
