package com.gy.smartorder.dtos.coupon

import com.gy.smartorder.entities.coupon.Coupon
import java.time.LocalDateTime

class CouponDto {

    /** 프론트 `Coupon`({ id, name, discountAmount, expiresAt })과 1:1. */
    data class CouponResponse(
        val id: String,
        val name: String,
        val discountAmount: Int,
        val expiresAt: LocalDateTime,
    ) {
        companion object {
            fun from(coupon: Coupon): CouponResponse = CouponResponse(
                id = coupon.id.toString(),
                name = coupon.name,
                discountAmount = coupon.discountAmount,
                expiresAt = coupon.expiresAt,
            )
        }
    }
}
