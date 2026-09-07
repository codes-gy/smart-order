package com.gy.smartorder.dtos.auth

import com.gy.smartorder.entities.member.Member
import com.gy.smartorder.entities.member.SocialProvider
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class AuthDto {

    data class SignUpRequest(
        @field:NotBlank(message = "이메일은 필수입니다.")
        @field:Email(message = "올바른 이메일 형식이 아닙니다.")
        val email: String,

        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
        val password: String,

        @field:NotBlank(message = "닉네임은 필수입니다.")
        @field:Size(min = 2, max = 50, message = "닉네임은 2~50자 사이여야 합니다.")
        val nickname: String,

        @field:Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        val phoneNumber: String? = null
    )

    data class LoginRequest(
        @field:NotBlank(message = "이메일은 필수입니다.")
        @field:Email(message = "올바른 이메일 형식이 아닙니다.")
        val email: String,

        @field:NotBlank(message = "비밀번호는 필수입니다.")
        val password: String
    )

    // frontend/src/types/auth.types.ts의 SocialLoginRequest와 1:1 (provider, accessToken 필드명 일치)
    data class SocialLoginRequest(
        val provider: SocialProvider,
        @field:NotBlank(message = "소셜 액세스 토큰은 필수입니다.")
        val accessToken: String,
        val nickname: String? = null,
    )

    data class RefreshTokenRequest(
        @field:NotBlank(message = "Refresh Token은 필수입니다.")
        val refreshToken: String
    )

    // frontend AuthTokens와 1:1
    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String
    )

    data class RefreshResponse(
        val accessToken: String
    )

    // frontend AuthUser와 1:1 (Member.nickname → name, Long id → String id로 변환)
    data class AuthUserResponse(
        val id: String,
        val name: String,
        val phoneNumber: String?,
        val isGuest: Boolean,
    ) {
        companion object {
            fun from(member: Member, isGuest: Boolean = false): AuthUserResponse =
                AuthUserResponse(
                    id = requireNotNull(member.id).toString(),
                    name = member.nickname,
                    phoneNumber = member.phoneNumber,
                    isGuest = isGuest,
                )
        }
    }

    // frontend authApi.socialLogin()의 Promise<{ user: AuthUser; tokens: AuthTokens }>와 1:1
    data class SocialLoginResponse(
        val user: AuthUserResponse,
        val tokens: LoginResponse,
    )

    // frontend authApi.sendSms(phoneNumber)와 1:1
    data class SendSmsRequest(
        @field:NotBlank(message = "휴대폰 번호는 필수입니다.")
        val phoneNumber: String
    )

    // frontend authApi.sendSms()의 Promise<{ success: true }>와 1:1
    data class SendSmsResponse(
        val success: Boolean
    )

    // frontend authApi.verifySms(phoneNumber, code)와 1:1 (code는 4자리 숫자, otpSchema.length(4))
    data class VerifySmsRequest(
        @field:NotBlank(message = "휴대폰 번호는 필수입니다.")
        val phoneNumber: String,

        @field:NotBlank(message = "인증번호는 필수입니다.")
        val code: String
    )

    // frontend StoreLoginRequest와 1:1. Member 인증(email/social/sms)과는 완전히 분리된 매장 POS/태블릿 로그인.
    data class StoreLoginRequest(
        @field:NotBlank(message = "매장 코드는 필수입니다.")
        val storeCode: String,

        @field:NotBlank(message = "비밀번호는 필수입니다.")
        val password: String,
    )

    // frontend StoreSession과 1:1. refreshToken이 없다 — 매장 세션은 재로그인 방식(프론트 계약).
    data class StoreSessionResponse(
        val storeId: String,
        val storeName: String,
        val accessToken: String,
    )
}