package com.gy.smartorder.services.auth

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.UnauthorizedException
import com.gy.smartorder.config.passport.JwtTokenProvider
import com.gy.smartorder.dtos.auth.AuthDto
import com.gy.smartorder.entities.member.Member
import com.gy.smartorder.entities.member.MemberStatus
import com.gy.smartorder.entities.member.SocialProvider
import com.gy.smartorder.repositories.member.MemberRepository
import com.gy.smartorder.repositories.store.StoreAccountRepository
import com.gy.smartorder.repositories.store.StoreRepository
import com.gy.smartorder.services.auth.oauth.SocialTokenVerifier
import com.gy.smartorder.services.coupon.CouponService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration
import java.util.Date
import java.util.Optional

/**
 * AuthService를 실제 Spring 컨텍스트 없이 Mockito로만 검증하는 순수 단위 테스트.
 * JwtTokenProvider는 시크릿/만료시간 등 `@Value` 설정이 필요해 컨텍스트 없이 인스턴스화하기 번거로워
 * 실제 구현 대신 mock으로 대체하고, 토큰 문자열 자체는 의미 없는 더미로 취급한다.
 */
class AuthServiceTest {

    private lateinit var memberRepository: MemberRepository
    private lateinit var storeAccountRepository: StoreAccountRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var valueOperations: ValueOperations<String, String>
    private lateinit var kakaoTokenVerifier: SocialTokenVerifier
    private lateinit var couponService: CouponService
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        memberRepository = mock(MemberRepository::class.java)
        storeAccountRepository = mock(StoreAccountRepository::class.java)
        storeRepository = mock(StoreRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        jwtTokenProvider = mock(JwtTokenProvider::class.java)
        redisTemplate = mock(StringRedisTemplate::class.java)
        valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>
        given(redisTemplate.opsForValue()).willReturn(valueOperations)

        kakaoTokenVerifier = mock(SocialTokenVerifier::class.java)
        given(kakaoTokenVerifier.provider).willReturn(SocialProvider.KAKAO)
        couponService = mock(CouponService::class.java)

        authService = AuthService(
            memberRepository,
            storeAccountRepository,
            storeRepository,
            passwordEncoder,
            jwtTokenProvider,
            redisTemplate,
            couponService,
            listOf(kakaoTokenVerifier),
        )
    }

    private fun member(
        id: Long = 1L,
        email: String? = "user@example.com",
        password: String? = "encoded-password",
        nickname: String = "테스터",
        status: MemberStatus = MemberStatus.ACTIVE,
    ) = Member(id = id, email = email, password = password, nickname = nickname, status = status)

    @Test
    fun `회원가입에 성공하면 토큰 쌍을 발급한다`() {
        val request = AuthDto.SignUpRequest(
            email = "new@example.com",
            password = "password123",
            nickname = "신규회원",
            phoneNumber = null,
        )
        given(memberRepository.findByEmail(request.email)).willReturn(null)
        given(passwordEncoder.encode(request.password)).willReturn("encoded-password")
        given(memberRepository.save(any(Member::class.java))).willAnswer { invocation ->
            val saved = invocation.arguments[0] as Member
            Member(id = 1L, email = saved.email, password = saved.password, nickname = saved.nickname)
        }
        given(jwtTokenProvider.generateAccessToken(1L, "USER")).willReturn("access-token")
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token")

        val response = authService.signup(request)

        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo("refresh-token")
        verify(couponService).issueWelcomeCoupon(1L)
    }

    @Test
    fun `이미 가입된 이메일로 회원가입하면 ConflictException을 던진다`() {
        val request = AuthDto.SignUpRequest(
            email = "user@example.com",
            password = "password123",
            nickname = "회원",
            phoneNumber = null,
        )
        given(memberRepository.findByEmail(request.email)).willReturn(member())

        assertThatThrownBy { authService.signup(request) }
            .isInstanceOf(ConflictException::class.java)
        verify(memberRepository, never()).save(any(Member::class.java))
    }

    @Test
    fun `이메일 비밀번호가 일치하면 로그인에 성공한다`() {
        val request = AuthDto.LoginRequest(email = "user@example.com", password = "raw-password")
        val existing = member()
        given(memberRepository.findByEmail(request.email)).willReturn(existing)
        given(passwordEncoder.matches(request.password, existing.password)).willReturn(true)
        given(jwtTokenProvider.generateAccessToken(1L, "USER")).willReturn("access-token")
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token")

        val response = authService.login(request)

        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo("refresh-token")
    }

    @Test
    fun `비밀번호가 일치하지 않으면 로그인 시 UnauthorizedException을 던진다`() {
        val request = AuthDto.LoginRequest(email = "user@example.com", password = "wrong-password")
        val existing = member()
        given(memberRepository.findByEmail(request.email)).willReturn(existing)
        given(passwordEncoder.matches(request.password, existing.password)).willReturn(false)

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `탈퇴 처리된 계정은 비밀번호가 맞아도 이메일 로그인 시 UnauthorizedException을 던진다`() {
        val request = AuthDto.LoginRequest(email = "user@example.com", password = "raw-password")
        val existing = member(status = MemberStatus.INACTIVE)
        given(memberRepository.findByEmail(request.email)).willReturn(existing)
        given(passwordEncoder.matches(request.password, existing.password)).willReturn(true)

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(UnauthorizedException::class.java)
        verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString())
    }

    @Test
    fun `소셜 SMS 전용 계정으로 이메일 로그인을 시도하면 UnauthorizedException을 던진다`() {
        val request = AuthDto.LoginRequest(email = "social@example.com", password = "anything")
        given(memberRepository.findByEmail(request.email)).willReturn(member(email = "social@example.com", password = null))

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `존재하지 않는 이메일로 로그인하면 UnauthorizedException을 던진다`() {
        given(memberRepository.findByEmail("none@example.com")).willReturn(null)

        assertThatThrownBy {
            authService.login(AuthDto.LoginRequest(email = "none@example.com", password = "password"))
        }.isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `소셜 토큰 검증에 성공하면 검증된 socialId로 회원을 조회하고 토큰을 발급한다`() {
        val request = AuthDto.SocialLoginRequest(provider = SocialProvider.KAKAO, accessToken = "kakao-access-token")
        given(kakaoTokenVerifier.verify("kakao-access-token")).willReturn("verified-kakao-id")
        val existing = member(email = null, password = null)
        given(memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "verified-kakao-id"))
            .willReturn(existing)
        given(jwtTokenProvider.generateAccessToken(1L, "USER")).willReturn("access-token")
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token")

        val response = authService.socialLogin(request)

        assertThat(response.tokens.accessToken).isEqualTo("access-token")
        verify(memberRepository, never()).save(any(Member::class.java))
        verify(couponService, never()).issueWelcomeCoupon(anyLong())
    }

    @Test
    fun `소셜 토큰 검증에 성공했지만 신규 회원이면 회원을 생성하고 웰컴 쿠폰을 발급한다`() {
        val request = AuthDto.SocialLoginRequest(provider = SocialProvider.KAKAO, accessToken = "kakao-access-token")
        given(kakaoTokenVerifier.verify("kakao-access-token")).willReturn("new-kakao-id")
        given(memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "new-kakao-id"))
            .willReturn(null)
        given(memberRepository.save(any(Member::class.java))).willAnswer { invocation ->
            val saved = invocation.arguments[0] as Member
            Member(
                id = 2L,
                socialProvider = saved.socialProvider,
                socialId = saved.socialId,
                nickname = saved.nickname,
            )
        }
        given(jwtTokenProvider.generateAccessToken(2L, "USER")).willReturn("access-token")
        given(jwtTokenProvider.generateRefreshToken(2L)).willReturn("refresh-token")

        authService.socialLogin(request)

        verify(couponService).issueWelcomeCoupon(2L)
    }

    @Test
    fun `소셜 토큰 검증에 실패하면 UnauthorizedException이 그대로 전파된다`() {
        val request = AuthDto.SocialLoginRequest(provider = SocialProvider.KAKAO, accessToken = "forged-token")
        given(kakaoTokenVerifier.verify("forged-token"))
            .willThrow(UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다."))

        assertThatThrownBy { authService.socialLogin(request) }
            .isInstanceOf(UnauthorizedException::class.java)
        verifyNoInteractions(memberRepository)
    }

    @Test
    fun `지원하지 않는 소셜 제공자면 BadRequestException을 던진다`() {
        val request = AuthDto.SocialLoginRequest(provider = SocialProvider.GOOGLE, accessToken = "token")

        assertThatThrownBy { authService.socialLogin(request) }
            .isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun `SMS 인증 성공 시 신규 회원이면 게스트 회원을 생성하고 웰컴 쿠폰을 발급한다`() {
        val request = AuthDto.VerifySmsRequest(phoneNumber = "010-1234-5678", code = "1234")
        given(valueOperations.get("sms:auth:010-1234-5678")).willReturn("1234")
        given(memberRepository.findByPhoneNumber("010-1234-5678")).willReturn(null)
        given(memberRepository.save(any(Member::class.java))).willAnswer { invocation ->
            val saved = invocation.arguments[0] as Member
            Member(id = 3L, phoneNumber = saved.phoneNumber, nickname = saved.nickname)
        }
        given(jwtTokenProvider.generateAccessToken(3L, "USER")).willReturn("access-token")
        given(jwtTokenProvider.generateRefreshToken(3L)).willReturn("refresh-token")

        authService.verifySms(request)

        verify(couponService).issueWelcomeCoupon(3L)
    }

    @Test
    fun `SMS 인증 성공 시 기존 회원이면 웰컴 쿠폰을 다시 발급하지 않는다`() {
        val request = AuthDto.VerifySmsRequest(phoneNumber = "010-1234-5678", code = "1234")
        given(valueOperations.get("sms:auth:010-1234-5678")).willReturn("1234")
        given(memberRepository.findByPhoneNumber("010-1234-5678")).willReturn(member(email = null, password = null))
        given(jwtTokenProvider.generateAccessToken(1L, "USER")).willReturn("access-token")
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token")

        authService.verifySms(request)

        verify(memberRepository, never()).save(any(Member::class.java))
        verify(couponService, never()).issueWelcomeCoupon(anyLong())
    }

    @Test
    fun `유효한 리프레시 토큰으로 새 액세스 토큰을 발급한다`() {
        val token = "valid-refresh-token"
        val existing = member()
        given(jwtTokenProvider.validateToken(token)).willReturn(true)
        given(jwtTokenProvider.getTokenType(token)).willReturn(JwtTokenProvider.TOKEN_TYPE_REFRESH)
        given(jwtTokenProvider.getSubjectId(token)).willReturn(1L)
        given(memberRepository.findById(1L)).willReturn(Optional.of(existing))
        given(valueOperations.get("auth:logout:1")).willReturn(null)
        given(jwtTokenProvider.generateAccessToken(1L, "USER")).willReturn("new-access-token")

        val response = authService.refresh(AuthDto.RefreshTokenRequest(refreshToken = token))

        assertThat(response.accessToken).isEqualTo("new-access-token")
    }

    @Test
    fun `유효하지 않은 리프레시 토큰이면 UnauthorizedException을 던진다`() {
        val token = "invalid-token"
        given(jwtTokenProvider.validateToken(token)).willReturn(false)

        assertThatThrownBy {
            authService.refresh(AuthDto.RefreshTokenRequest(refreshToken = token))
        }.isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `access 타입 토큰으로 refresh를 호출하면 UnauthorizedException을 던진다`() {
        val token = "access-token-used-as-refresh"
        given(jwtTokenProvider.validateToken(token)).willReturn(true)
        given(jwtTokenProvider.getTokenType(token)).willReturn(JwtTokenProvider.TOKEN_TYPE_ACCESS)

        assertThatThrownBy {
            authService.refresh(AuthDto.RefreshTokenRequest(refreshToken = token))
        }.isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `로그아웃 시점 이전에 발급된 리프레시 토큰은 무효화된다`() {
        val token = "refresh-token-before-logout"
        val existing = member()
        given(jwtTokenProvider.validateToken(token)).willReturn(true)
        given(jwtTokenProvider.getTokenType(token)).willReturn(JwtTokenProvider.TOKEN_TYPE_REFRESH)
        given(jwtTokenProvider.getSubjectId(token)).willReturn(1L)
        given(memberRepository.findById(1L)).willReturn(Optional.of(existing))
        given(jwtTokenProvider.getIssuedAt(token)).willReturn(Date(1_000L))
        given(valueOperations.get("auth:logout:1")).willReturn("2000")

        assertThatThrownBy {
            authService.refresh(AuthDto.RefreshTokenRequest(refreshToken = token))
        }.isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `탈퇴한 회원의 리프레시 토큰은 거부된다`() {
        val token = "refresh-token-inactive-member"
        given(jwtTokenProvider.validateToken(token)).willReturn(true)
        given(jwtTokenProvider.getTokenType(token)).willReturn(JwtTokenProvider.TOKEN_TYPE_REFRESH)
        given(jwtTokenProvider.getSubjectId(token)).willReturn(1L)
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(status = MemberStatus.INACTIVE)))

        assertThatThrownBy {
            authService.refresh(AuthDto.RefreshTokenRequest(refreshToken = token))
        }.isInstanceOf(UnauthorizedException::class.java)
    }

    @Test
    fun `로그아웃하면 해당 회원의 로그아웃 시점이 Redis에 기록된다`() {
        given(jwtTokenProvider.refreshTokenTtlMillis()).willReturn(1_209_600_000L)

        authService.logout(1L)

        verify(valueOperations).set(eq("auth:logout:1"), anyString(), any(Duration::class.java))
    }

    @Test
    fun `principal이 없으면 로그아웃은 아무 것도 하지 않는다`() {
        authService.logout(null)

        verify(redisTemplate, never()).opsForValue()
    }
}
