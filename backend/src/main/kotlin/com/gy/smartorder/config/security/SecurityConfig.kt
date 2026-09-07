package com.gy.smartorder.config.security

import com.gy.smartorder.config.passport.JwtAuthenticationEntryPoint
import com.gy.smartorder.config.passport.JwtAuthenticationFilter
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
            allowedOriginPatterns = listOf("*") // 운영 환경에서는 허용 도메인 명시 권장
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
