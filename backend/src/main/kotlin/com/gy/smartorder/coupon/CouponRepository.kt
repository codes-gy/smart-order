package com.gy.smartorder.coupon

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface CouponRepository : JpaRepository<Coupon, Long> {
    fun findByMemberIdAndUsedAtIsNullAndExpiresAtAfterOrderByExpiresAtAsc(
        memberId: Long,
        now: LocalDateTime,
    ): List<Coupon>

    fun findByIdAndMemberId(id: Long, memberId: Long): Coupon?
}
