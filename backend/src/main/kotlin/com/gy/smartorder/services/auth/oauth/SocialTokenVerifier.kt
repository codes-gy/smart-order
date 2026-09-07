package com.gy.smartorder.services.auth.oauth

import com.gy.smartorder.entities.member.SocialProvider

/**
 * 소셜 로그인 제공자가 발급한 토큰을 그 제공자의 서버에 검증하고, 검증된 사용자 식별자(socialId)를 돌려준다.
 * 검증 실패(위조/만료/형식 오류 등) 시 [com.gy.smartorder.common.exception.UnauthorizedException]을 던진다.
 */
interface SocialTokenVerifier {
    val provider: SocialProvider

    fun verify(token: String): String
}
