package com.gy.smartorder.auth

import com.gy.smartorder.config.passport.JwtTokenProvider
import com.gy.smartorder.auth.AuthDto
import com.gy.smartorder.auth.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody req: AuthDto.SignUpRequest): ResponseEntity<AuthDto.LoginResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(req))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: AuthDto.LoginRequest): ResponseEntity<AuthDto.LoginResponse> {
        return ResponseEntity.ok(authService.login(req))
    }

    // frontend authApi.socialLogin()이 apiClient로 교체될 때 연동될 엔드포인트
    @PostMapping("/social-login")
    fun socialLogin(@Valid @RequestBody req: AuthDto.SocialLoginRequest): ResponseEntity<AuthDto.SocialLoginResponse> {
        return ResponseEntity.ok(authService.socialLogin(req))
    }

    // /auth/**가 permitAll이라 토큰 없이도 호출할 수 있다 — Authorization 헤더가 있으면
    // JwtAuthenticationFilter가 principal(subjectId)을 채워두고, 없으면 null(AuthService.logout()이 처리).
    // subjectId는 Member 토큰이면 memberId, STORE_ADMIN 토큰이면 storeId라 같은 숫자 공간을 공유한다 —
    // 매장 관리자 토큰(리프레시 토큰 자체가 없어 무효화할 대상도 없음)을 memberId로 착각해 넘기면 우연히
    // 같은 id의 무관한 회원 리프레시 토큰을 무효화시키는 사고가 나므로, role이 STORE_ADMIN이면 로그아웃 처리를 건너뛴다.
    @PostMapping("/logout")
    fun logout(authentication: Authentication?): ResponseEntity<Void> {
        val isStoreAdmin = authentication?.authorities
            ?.any { it.authority == "ROLE_${JwtTokenProvider.ROLE_STORE_ADMIN}" } ?: false
        val memberId = (authentication?.principal as? Long)?.takeUnless { isStoreAdmin }
        authService.logout(memberId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody req: AuthDto.RefreshTokenRequest): ResponseEntity<AuthDto.RefreshResponse> {
        return ResponseEntity.ok(authService.refresh(req))
    }

    @PostMapping("/sms/send")
    fun sendSms(@Valid @RequestBody req: AuthDto.SendSmsRequest): ResponseEntity<AuthDto.SendSmsResponse> {
        return ResponseEntity.ok(authService.sendSms(req))
    }

    @PostMapping("/sms/verify")
    fun verifySms(@Valid @RequestBody req: AuthDto.VerifySmsRequest): ResponseEntity<AuthDto.SocialLoginResponse> {
        return ResponseEntity.ok(authService.verifySms(req))
    }

    // frontend authApi.storeLogin()이 apiClient로 교체될 때 연동될 엔드포인트
    @PostMapping("/store-login")
    fun storeLogin(@Valid @RequestBody req: AuthDto.StoreLoginRequest): ResponseEntity<AuthDto.StoreSessionResponse> {
        return ResponseEntity.ok(authService.storeLogin(req))
    }

}