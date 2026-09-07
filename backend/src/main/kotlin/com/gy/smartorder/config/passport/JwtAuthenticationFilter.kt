package com.gy.smartorder.config.passport

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authorization: Bearer 헤더의 액세스 토큰을 검증해 SecurityContext에 인증 정보를 채워 넣는다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 통과시키고, 이후 `authorizeHttpRequests` 규칙이
 * permitAll이 아닌 경로에 대해 401/403으로 처리하도록 맡긴다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null &&
            jwtTokenProvider.validateToken(token) &&
            jwtTokenProvider.getTokenType(token) == JwtTokenProvider.TOKEN_TYPE_ACCESS
        ) {
            val subjectId = jwtTokenProvider.getSubjectId(token)
            val role = jwtTokenProvider.getRole(token) ?: "USER"
            val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
            val authentication = UsernamePasswordAuthenticationToken(subjectId, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization") ?: return null
        return if (bearerToken.startsWith(BEARER_PREFIX)) bearerToken.substring(BEARER_PREFIX.length) else null
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
