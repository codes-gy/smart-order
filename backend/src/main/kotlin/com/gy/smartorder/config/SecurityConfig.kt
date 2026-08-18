package com.gy.smartorder.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * 임시 보안 설정.
 *
 * `spring-boot-starter-security`가 클래스패스에 있으면 별도 설정이 없는 한 스프링 시큐리티가
 * 모든 요청에 기본 HTTP Basic 인증(부팅 시 랜덤 생성되는 비밀번호)을 강제한다. 인증 도메인(B1)이
 * 아직 없는 지금 상태로는 프론트/테스트가 API를 아예 호출할 수 없게 되므로, 우선 전체 요청을
 * permitAll로 열어 둔다.
 *
 * TODO(B1 인증 도메인 구현 시): JWT 필터를 추가하고, 매장 조회(GET)는 permitAll을 유지하되
 * 매장 정보 변경(POST/PATCH)은 매장 관리자 인증(hasRole("STORE_ADMIN"))으로 제한할 것.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            httpBasic { disable() }
            formLogin { disable() }
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }
}
