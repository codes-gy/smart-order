package com.gy.smartorder.controllers.auth

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.UnauthorizedException
import com.gy.smartorder.config.passport.JwtAuthenticationEntryPoint
import com.gy.smartorder.config.passport.JwtTokenProvider
import com.gy.smartorder.config.security.SecurityConfig
import com.gy.smartorder.dtos.auth.AuthDto
import com.gy.smartorder.services.auth.AuthService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `/auth` 하위 전체 경로는 SecurityConfig에서 permitAll이라 인증 헤더 없이도 호출 가능해야 한다
 * (StoreControllerTest와 동일한 이유로 SecurityConfig를 `@Import` — 슬라이스가 기본 시큐리티로 모든 요청을 막는 것을 방지).
 * `JwtAuthenticationFilter`가 `JwtTokenProvider`를 요구하는데 슬라이스엔 그 빈이 없어 `@MockitoBean`으로 채워준다.
 */
@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, JwtAuthenticationEntryPoint::class)
class AuthControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    fun `회원가입에 성공하면 201과 토큰을 반환한다`() {
        given(authService.signup(AuthDto.SignUpRequest("new@example.com", "password123", "닉네임", null)))
            .willReturn(AuthDto.LoginResponse(accessToken = "access-token", refreshToken = "refresh-token"))

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"email":"new@example.com","password":"password123","nickname":"닉네임"}""",
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
    }

    @Test
    fun `이미 가입된 이메일로 회원가입하면 409와 ApiErrorBody를 반환한다`() {
        given(authService.signup(AuthDto.SignUpRequest("dup@example.com", "password123", "닉네임", null)))
            .willThrow(ConflictException("EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."))

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"email":"dup@example.com","password":"password123","nickname":"닉네임"}""",
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
    }

    @Test
    fun `이메일 형식이 올바르지 않으면 400을 반환한다`() {
        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"email":"not-an-email","password":"password123","nickname":"닉네임"}""",
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `로그인에 성공하면 200과 토큰을 반환한다`() {
        given(authService.login(AuthDto.LoginRequest("user@example.com", "password123")))
            .willReturn(AuthDto.LoginResponse(accessToken = "access-token", refreshToken = "refresh-token"))

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"password123"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
    }

    @Test
    fun `로그인 자격증명이 올바르지 않으면 401을 반환한다`() {
        given(authService.login(AuthDto.LoginRequest("user@example.com", "wrong-password")))
            .willThrow(UnauthorizedException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."))

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"wrong-password"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    fun `리프레시 토큰으로 새 액세스 토큰을 발급받는다`() {
        given(authService.refresh(AuthDto.RefreshTokenRequest("refresh-token")))
            .willReturn(AuthDto.RefreshResponse(accessToken = "new-access-token"))

        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"refresh-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
    }

    @Test
    fun `무효화된 리프레시 토큰이면 401을 반환한다`() {
        given(authService.refresh(AuthDto.RefreshTokenRequest("stale-token")))
            .willThrow(UnauthorizedException("INVALID_REFRESH_TOKEN", "리프레시 토큰이 유효하지 않습니다."))

        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"stale-token"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
    }

    @Test
    fun `Authorization 헤더 없이 로그아웃해도 204를 반환하고 principal은 null로 전달된다`() {
        willDoNothing().given(authService).logout(null)

        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isNoContent)

        verify(authService).logout(null)
    }

    @Test
    fun `Authorization 헤더의 액세스 토큰으로 로그아웃하면 해당 memberId로 무효화한다`() {
        val token = "valid-access-token"
        given(jwtTokenProvider.validateToken(token)).willReturn(true)
        given(jwtTokenProvider.getTokenType(token)).willReturn(JwtTokenProvider.TOKEN_TYPE_ACCESS)
        given(jwtTokenProvider.getSubjectId(token)).willReturn(42L)
        given(jwtTokenProvider.getRole(token)).willReturn("USER")
        willDoNothing().given(authService).logout(42L)

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)

        verify(authService).logout(42L)
    }

    @Test
    fun `매장 관리자 토큰으로 로그아웃하면 storeId를 memberId로 넘기지 않는다`() {
        val token = "store-admin-access-token"
        given(jwtTokenProvider.validateToken(token)).willReturn(true)
        given(jwtTokenProvider.getTokenType(token)).willReturn(JwtTokenProvider.TOKEN_TYPE_ACCESS)
        given(jwtTokenProvider.getSubjectId(token)).willReturn(42L)
        given(jwtTokenProvider.getRole(token)).willReturn(JwtTokenProvider.ROLE_STORE_ADMIN)
        willDoNothing().given(authService).logout(null)

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)

        verify(authService).logout(null)
        verify(authService, org.mockito.Mockito.never()).logout(42L)
    }
}
