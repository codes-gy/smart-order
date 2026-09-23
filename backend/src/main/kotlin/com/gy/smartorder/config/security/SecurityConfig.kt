package com.gy.smartorder.config.security

import com.gy.smartorder.config.passport.JwtAccessDeniedHandler
import com.gy.smartorder.config.passport.JwtAuthenticationEntryPoint
import com.gy.smartorder.config.passport.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * JWT 기반 인증 설정. 세션을 쓰지 않고(STATELESS) `JwtAuthenticationFilter`가
 * `UsernamePasswordAuthenticationFilter` 앞에서 Authorization 헤더의 액세스 토큰을 검증한다.
 */
@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAccessDeniedHandler: JwtAccessDeniedHandler,
    @Value("\${cors.allowed-origins}") private val corsAllowedOrigins: String,

) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors {}
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            httpBasic { disable() }
            formLogin { disable() }
            headers { frameOptions { sameOrigin = true } }
            exceptionHandling {
                authenticationEntryPoint = jwtAuthenticationEntryPoint
                accessDeniedHandler = jwtAccessDeniedHandler

            }
            authorizeHttpRequests {
                authorize("/auth/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/docs/**", permitAll)
                authorize("/h2-console/**", permitAll)
                authorize("/actuator/**", permitAll)

                authorize("/admin/**", hasRole("ADMIN"))

                // 매장 목록/상세 조회(PRD B)는 로그인 전 고객도 매장을 둘러볼 수 있어야 해서 공개.
                authorize(HttpMethod.GET, "/stores/**", permitAll)

                // 매장 정보 변경은 해당 매장 로그인(POST /auth/store-login)으로 발급된 STORE_ADMIN 토큰만 허용.
                // 매장 생성(POST)은 범위 밖 — PROGRESS.md 4절 5번 참고.
                authorize(HttpMethod.PATCH, "/stores/**", hasRole("STORE_ADMIN"))

                // 카테고리/메뉴/메뉴 옵션 변경도 매장 정보 변경과 동일하게 STORE_ADMIN 토큰만 허용.
                // 조회(GET)는 로그인한 회원이면 그대로 볼 수 있도록 손대지 않음(기존 동작 유지).
                authorize(HttpMethod.POST, "/stores/*/categories", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.PATCH, "/categories/**", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.DELETE, "/categories/**", hasRole("STORE_ADMIN"))

                authorize(HttpMethod.POST, "/menus", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.PUT, "/menus/**", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.PATCH, "/menus/**", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.DELETE, "/menus/**", hasRole("STORE_ADMIN"))

                authorize(HttpMethod.POST, "/menus/*/option-groups", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.PATCH, "/option-groups/**", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.DELETE, "/option-groups/**", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.POST, "/option-groups/*/choices", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.PATCH, "/option-choices/**", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.DELETE, "/option-choices/**", hasRole("STORE_ADMIN"))

                // 주문 상태 변경/매장별 주문 큐 조회는 STORE_ADMIN만 — 어느 매장인지는 서비스단에서
                // principal(storeId)과 대조해 다른 매장 주문에는 접근 못하게 한다 (2026-09 보안 점검).
                authorize(HttpMethod.PATCH, "/orders/*/status", hasRole("STORE_ADMIN"))
                authorize(HttpMethod.GET, "/orders/store/**", hasRole("STORE_ADMIN"))

                authorize(anyRequest, authenticated)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // 와일드카드(allowedOriginPatterns("*")) + allowCredentials(true) 조합은 임의의 외부
            // 사이트가 쿠키/Authorization 헤더를 포함한 크로스 오리진 요청을 보내도 브라우저가 이를
            // 허용해버려 CORS 보호가 사실상 무력화된다. cors.allowed-origins(CORS_ALLOWED_ORIGINS)로
            // 프로필별 허용 도메인을 명시적으로 관리한다 (쉼표로 구분해 여러 개 지정 가능).
            allowedOrigins = corsAllowedOrigins.split(",").map { it.trim() }.filter { it.isNotBlank() }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
