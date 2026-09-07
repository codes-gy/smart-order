package com.gy.smartorder.services.auth.oauth

import com.gy.smartorder.common.exception.UnauthorizedException
import com.gy.smartorder.entities.member.SocialProvider
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.LocatorAdapter
import io.jsonwebtoken.security.JwkSet
import io.jsonwebtoken.security.Jwks
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.security.Key
import java.time.Duration
import java.time.Instant

/**
 * Apple `identityToken`(JWT)을 애플 공개 JWKS(`/auth/keys`)로 서명 검증하고, `iss`/`aud`/`exp`까지 확인한다.
 * 검증되면 `sub` 클레임(애플 고유 사용자 식별자)을 socialId로 사용한다.
 * `oauth.apple.client-id`는 Sign in with Apple 콘솔에 등록한 Services ID(또는 Bundle ID) — 토큰의 `aud`와 일치해야 함.
 */
@Component
class AppleTokenVerifier(
    restClientBuilder: RestClient.Builder,
    @Value("\${oauth.apple.client-id:}") private val appleClientId: String,
) : SocialTokenVerifier {

    override val provider = SocialProvider.APPLE

    companion object {
        private const val ISSUER = "https://appleid.apple.com"
        private const val JWKS_PATH = "/auth/keys"
        private val JWKS_CACHE_TTL: Duration = Duration.ofHours(1)
    }

    private val restClient = restClientBuilder
        .baseUrl("https://appleid.apple.com")
        .build()

    @Volatile
    private var cachedJwkSet: JwkSet? = null

    @Volatile
    private var cachedAt: Instant = Instant.MIN

    override fun verify(token: String): String {
        check(appleClientId.isNotBlank()) {
            "oauth.apple.client-id 설정이 비어있습니다 (APPLE_CLIENT_ID 환경변수 확인 필요)."
        }

        val claims = try {
            Jwts.parser()
                .keyLocator(AppleSigningKeyLocator())
                .requireIssuer(ISSUER)
                .requireAudience(appleClientId)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: JwtException) {
            throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다.")
        } catch (e: IllegalArgumentException) {
            throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다.")
        }

        return claims.subject
            ?: throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다.")
    }

    private fun resolveKey(kid: String?): Key {
        if (kid == null) {
            throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다.")
        }
        val fromCache = jwkSet(forceRefresh = false).getKeys().find { it.getId() == kid }
        // 애플이 키를 순환(rotate)했을 수 있으니, 캐시에 없으면 한 번 강제 갱신 후 재시도한다.
        val jwk = fromCache ?: jwkSet(forceRefresh = true).getKeys().find { it.getId() == kid }
            ?: throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다.")
        return jwk.toKey()
    }

    @Synchronized
    private fun jwkSet(forceRefresh: Boolean): JwkSet {
        val cached = cachedJwkSet
        if (!forceRefresh && cached != null && Duration.between(cachedAt, Instant.now()) < JWKS_CACHE_TTL) {
            return cached
        }

        val json = try {
            restClient.get().uri(JWKS_PATH).retrieve().body(String::class.java)
        } catch (e: RestClientException) {
            throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰을 검증할 수 없습니다.")
        } ?: throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰을 검증할 수 없습니다.")

        val parsed = Jwks.setParser().build().parse(json)
        cachedJwkSet = parsed
        cachedAt = Instant.now()
        return parsed
    }

    private inner class AppleSigningKeyLocator : LocatorAdapter<Key>() {
        override fun locate(header: JwsHeader): Key = resolveKey(header.keyId)
    }
}
