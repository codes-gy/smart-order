package com.gy.smartorder.services.auth

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.UnauthorizedException
import com.gy.smartorder.config.passport.JwtTokenProvider
import com.gy.smartorder.dtos.auth.AuthDto
import com.gy.smartorder.entities.member.Member
import com.gy.smartorder.entities.member.MemberStatus
import com.gy.smartorder.repositories.member.MemberRepository
import com.gy.smartorder.repositories.store.StoreAccountRepository
import com.gy.smartorder.repositories.store.StoreRepository
import com.gy.smartorder.services.auth.oauth.SocialTokenVerifier
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional(readOnly = true)
class AuthService(
    private val memberRepository: MemberRepository,
    private val storeAccountRepository: StoreAccountRepository,
    private val storeRepository: StoreRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisTemplate: StringRedisTemplate,
    socialTokenVerifiers: List<SocialTokenVerifier>,
) {

    private val socialTokenVerifiersByProvider = socialTokenVerifiers.associateBy { it.provider }

    companion object {
        private val log = LoggerFactory.getLogger(AuthService::class.java)
        private const val SMS_CODE_KEY_PREFIX = "sms:auth:"
        private val SMS_CODE_TTL: Duration = Duration.ofMinutes(3)
        private val PHONE_NUMBER_REGEX = Regex("^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$")
        private const val LOGOUT_KEY_PREFIX = "auth:logout:"
    }

    // 로그아웃 시점을 memberId 기준으로 기록해두고, refresh()에서 그 시점 이전에 발급된 리프레시 토큰을
    // 전부 무효화한다(토큰 자체를 저장하지 않는 방식 — 리프레시 토큰 문자열을 서버에 남기지 않아도 됨).
    // 프론트 authApi.logout()이 인자를 받지 않으므로(=특정 refreshToken을 지목할 수 없으므로) 이 회원의
    // 모든 리프레시 토큰을 한 번에 무효화하는 방식을 택했다. TTL은 리프레시 토큰 최대 수명과 동일하게 둬서,
    // 그 이후엔 어차피 토큰 자체가 만료되므로 키를 계속 들고 있을 필요가 없다.
    @Transactional
    fun logout(memberId: Long?) {
        if (memberId == null) return
        redisTemplate.opsForValue().set(
            LOGOUT_KEY_PREFIX + memberId,
            System.currentTimeMillis().toString(),
            Duration.ofMillis(jwtTokenProvider.refreshTokenTtlMillis()),
        )
    }

    @Transactional
    fun signup(request: AuthDto.SignUpRequest): AuthDto.LoginResponse {
        if (memberRepository.findByEmail(request.email) != null) {
            throw ConflictException("EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.")
        }

        val member = memberRepository.save(
            Member(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                nickname = request.nickname,
                phoneNumber = request.phoneNumber,
            )
        )

        val memberId = requireNotNull(member.id)
        return AuthDto.LoginResponse(
            accessToken = jwtTokenProvider.generateAccessToken(memberId, member.role.name),
            refreshToken = jwtTokenProvider.generateRefreshToken(memberId),
        )
    }

    fun login(request: AuthDto.LoginRequest): AuthDto.LoginResponse {
        // 이메일/비밀번호 계정이 아닌 회원(소셜·SMS 전용)은 password가 null이라 matches()까지 갈 필요 없이 여기서 걸러진다.
        val member = memberRepository.findByEmail(request.email)
            ?.takeIf { it.password != null }
            ?: throw UnauthorizedException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.")

        if (!passwordEncoder.matches(request.password, member.password)) {
            throw UnauthorizedException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        if (member.status != MemberStatus.ACTIVE) {
            throw UnauthorizedException("MEMBER_NOT_ACTIVE", "이용이 제한된 계정입니다.")
        }

        val memberId = requireNotNull(member.id)
        return AuthDto.LoginResponse(
            accessToken = jwtTokenProvider.generateAccessToken(memberId, member.role.name),
            refreshToken = jwtTokenProvider.generateRefreshToken(memberId),
        )
    }

    @Transactional
    fun socialLogin(request: AuthDto.SocialLoginRequest): AuthDto.SocialLoginResponse {
        // 카카오는 /v1/user/access_token_info, 애플은 공개 JWKS로 accessToken을 각 제공자 서버에 검증하고
        // 그 결과로 받은 사용자 식별자를 socialId로 쓴다 — 클라이언트가 보낸 값은 신뢰하지 않는다.
        val verifier = socialTokenVerifiersByProvider[request.provider]
            ?: throw BadRequestException("UNSUPPORTED_SOCIAL_PROVIDER", "지원하지 않는 소셜 로그인 제공자입니다.")
        val socialId = verifier.verify(request.accessToken)

        val member = memberRepository.findBySocialProviderAndSocialId(request.provider, socialId)
            ?: memberRepository.save(
                Member(
                    socialProvider = request.provider,
                    socialId = socialId,
                    nickname = request.nickname ?: "${request.provider} 사용자",
                )
            )

        if (member.status != MemberStatus.ACTIVE) {
            throw UnauthorizedException("MEMBER_NOT_ACTIVE", "이용이 제한된 계정입니다.")
        }

        val memberId = requireNotNull(member.id)
        return AuthDto.SocialLoginResponse(
            user = AuthDto.AuthUserResponse.from(member, isGuest = false),
            tokens = AuthDto.LoginResponse(
                accessToken = jwtTokenProvider.generateAccessToken(memberId, member.role.name),
                refreshToken = jwtTokenProvider.generateRefreshToken(memberId),
            ),
        )
    }

    fun refresh(request: AuthDto.RefreshTokenRequest): AuthDto.RefreshResponse {
        val token = request.refreshToken
        if (!jwtTokenProvider.validateToken(token) ||
            jwtTokenProvider.getTokenType(token) != JwtTokenProvider.TOKEN_TYPE_REFRESH
        ) {
            throw UnauthorizedException("INVALID_REFRESH_TOKEN", "리프레시 토큰이 유효하지 않습니다.")
        }

        val member = memberRepository.findById(jwtTokenProvider.getSubjectId(token)).orElseThrow {
            UnauthorizedException("INVALID_REFRESH_TOKEN", "리프레시 토큰이 유효하지 않습니다.")
        }
        if (member.status != MemberStatus.ACTIVE) {
            throw UnauthorizedException("INVALID_REFRESH_TOKEN", "리프레시 토큰이 유효하지 않습니다.")
        }

        val memberId = requireNotNull(member.id)
        val loggedOutAt = redisTemplate.opsForValue().get(LOGOUT_KEY_PREFIX + memberId)?.toLong()
        if (loggedOutAt != null && jwtTokenProvider.getIssuedAt(token).time <= loggedOutAt) {
            throw UnauthorizedException("INVALID_REFRESH_TOKEN", "리프레시 토큰이 유효하지 않습니다.")
        }
        return AuthDto.RefreshResponse(
            accessToken = jwtTokenProvider.generateAccessToken(memberId, member.role.name),
        )
    }

    // 비회원 주문용 휴대폰 SMS OTP 로그인 플로우 (F-00). 실제 발송 벤더 연동 전까지는 콘솔 로그로만 코드를 노출하는 mock.
    fun sendSms(request: AuthDto.SendSmsRequest): AuthDto.SendSmsResponse {
        if (!PHONE_NUMBER_REGEX.matches(request.phoneNumber)) {
            throw BadRequestException("INVALID_PHONE", "올바른 휴대폰 번호 형식이 아닙니다.")
        }

        val code = (1000..9999).random().toString()
        redisTemplate.opsForValue().set(SMS_CODE_KEY_PREFIX + request.phoneNumber, code, SMS_CODE_TTL)
        log.info("[SMS mock] phoneNumber={} code={}", request.phoneNumber, code)

        return AuthDto.SendSmsResponse(success = true)
    }

    // 인증 성공 시 phoneNumber 기준 find-or-create. 재방문 여부와 무관하게 isGuest는 항상 true로 내려준다(프론트 계약, 소셜 로그인 isGuest=false와 대비).
    @Transactional
    fun verifySms(request: AuthDto.VerifySmsRequest): AuthDto.SocialLoginResponse {
        val key = SMS_CODE_KEY_PREFIX + request.phoneNumber
        val savedCode = redisTemplate.opsForValue().get(key)
        if (savedCode == null || savedCode != request.code) {
            throw BadRequestException("INVALID_OTP", "인증번호가 올바르지 않거나 만료되었습니다.")
        }
        redisTemplate.delete(key)

        val member = memberRepository.findByPhoneNumber(request.phoneNumber)
            ?: memberRepository.save(
                Member(
                    phoneNumber = request.phoneNumber,
                    nickname = "게스트",
                )
            )

        if (member.status != MemberStatus.ACTIVE) {
            throw UnauthorizedException("MEMBER_NOT_ACTIVE", "이용이 제한된 계정입니다.")
        }

        val memberId = requireNotNull(member.id)
        return AuthDto.SocialLoginResponse(
            user = AuthDto.AuthUserResponse.from(member, isGuest = true),
            tokens = AuthDto.LoginResponse(
                accessToken = jwtTokenProvider.generateAccessToken(memberId, member.role.name),
                refreshToken = jwtTokenProvider.generateRefreshToken(memberId),
            ),
        )
    }

    // 매장 POS/태블릿 로그인. Member 인증과 완전히 분리된 별도 principal(StoreAccount) — 회원가입 플로우 없이
    // 계정은 사전 프로비저닝된다고 가정한다(현재 이 계정을 만드는 API 자체가 없음, PROGRESS.md 참고).
    fun storeLogin(request: AuthDto.StoreLoginRequest): AuthDto.StoreSessionResponse {
        val storeAccount = storeAccountRepository.findByStoreCode(request.storeCode)
            ?: throw UnauthorizedException("INVALID_CREDENTIALS", "매장 코드 또는 비밀번호가 올바르지 않습니다.")

        if (!passwordEncoder.matches(request.password, storeAccount.password)) {
            throw UnauthorizedException("INVALID_CREDENTIALS", "매장 코드 또는 비밀번호가 올바르지 않습니다.")
        }

        val store = storeRepository.findById(storeAccount.storeId).orElseThrow {
            UnauthorizedException("INVALID_CREDENTIALS", "매장 코드 또는 비밀번호가 올바르지 않습니다.")
        }

        return AuthDto.StoreSessionResponse(
            storeId = storeAccount.storeId.toString(),
            storeName = store.name,
            accessToken = jwtTokenProvider.generateAccessToken(storeAccount.storeId, JwtTokenProvider.ROLE_STORE_ADMIN),
        )
    }
}
