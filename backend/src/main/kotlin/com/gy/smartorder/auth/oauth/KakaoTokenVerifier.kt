package com.gy.smartorder.auth.oauth

import com.gy.smartorder.common.exception.UnauthorizedException
import com.gy.smartorder.member.SocialProvider
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * 카카오 액세스 토큰을 카카오 서버(`/v1/user/access_token_info`)에 검증한다.
 * 토큰이 유효하면 카카오가 응답한 회원 번호(`id`)를 socialId로 사용한다 — 클라이언트가 보낸 값은 신뢰하지 않음.
 */
@Component
class KakaoTokenVerifier(
    restClientBuilder: RestClient.Builder,
) : SocialTokenVerifier {

    override val provider = SocialProvider.KAKAO

    private val restClient = restClientBuilder
        .baseUrl("https://kapi.kakao.com")
        .build()

    override fun verify(token: String): String {
        val response = try {
            restClient.get()
                .uri("/v1/user/access_token_info")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .body(AccessTokenInfoResponse::class.java)
        } catch (e: RestClientException) {
            throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다.")
        }

        val kakaoUserId = response?.id
            ?: throw UnauthorizedException("INVALID_SOCIAL_TOKEN", "소셜 로그인 토큰이 유효하지 않습니다.")
        return kakaoUserId.toString()
    }

    private data class AccessTokenInfoResponse(val id: Long)
}
