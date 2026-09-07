package com.gy.smartorder.config.passport

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}") private val refreshTokenExpiration: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    companion object {
        const val TOKEN_TYPE_ACCESS = "access"
        const val TOKEN_TYPE_REFRESH = "refresh"
        const val ROLE_STORE_ADMIN = "STORE_ADMIN"
        private const val CLAIM_ROLE = "role"
        private const val CLAIM_TYPE = "type"
    }

    // role은 Member.Role에 묶이지 않은 원시 문자열이다 — Member(USER/ADMIN)와 매장 계정(STORE_ADMIN)처럼
    // principal 종류가 다른 발급 대상을 이 하나의 프로바이더가 함께 지원하기 위함(subject도 memberId/storeId 등
    // 발급 대상에 따라 의미가 달라짐, 어떤 principal인지는 role 클레임으로 구분).
    fun generateAccessToken(id: Long, role: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(id.toString())
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenExpiration))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(id: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(id.toString())
            .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
            .issuedAt(now)
            .expiration(Date(now.time + refreshTokenExpiration))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

    fun getSubjectId(token: String): Long = parseClaims(token).subject.toLong()

    fun getRole(token: String): String? = parseClaims(token).get(CLAIM_ROLE, String::class.java)

    fun getTokenType(token: String): String? = parseClaims(token).get(CLAIM_TYPE, String::class.java)

    fun getIssuedAt(token: String): Date = parseClaims(token).issuedAt

    fun refreshTokenTtlMillis(): Long = refreshTokenExpiration

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
