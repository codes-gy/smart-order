package com.gy.smartorder.services.coupon

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.coupon.CouponDto
import com.gy.smartorder.entities.coupon.Coupon
import com.gy.smartorder.repositories.coupon.CouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class CouponService(
    private val couponRepository: CouponRepository,
) {
    companion object {
        private const val WELCOME_COUPON_NAME = "웰컴 2,000원 할인 쿠폰"
        private const val WELCOME_COUPON_DISCOUNT_AMOUNT = 2000
        private const val WELCOME_COUPON_VALID_DAYS = 30L
    }

    /** 신규 회원 최초 생성(provision) 시 한 번 발급되는 웰컴 쿠폰. */
    @Transactional
    fun issueWelcomeCoupon(memberId: Long) {
        couponRepository.save(
            Coupon(
                memberId = memberId,
                name = WELCOME_COUPON_NAME,
                discountAmount = WELCOME_COUPON_DISCOUNT_AMOUNT,
                expiresAt = LocalDateTime.now().plusDays(WELCOME_COUPON_VALID_DAYS),
            )
        )
    }

    fun getAvailableCoupons(memberId: Long): List<CouponDto.CouponResponse> =
        couponRepository
            .findByMemberIdAndUsedAtIsNullAndExpiresAtAfterOrderByExpiresAtAsc(memberId, LocalDateTime.now())
            .map { CouponDto.CouponResponse.from(it) }

    /** 주문 생성 시 쿠폰을 소비하고 할인액을 반환한다. 본인 소유가 아니거나 이미 쓴 쿠폰, 만료된 쿠폰은 거부한다. */
    @Transactional
    fun redeem(memberId: Long, couponId: Long): Int {
        val coupon = couponRepository.findByIdAndMemberId(couponId, memberId)
            ?: throw NotFoundException("COUPON_NOT_FOUND", "해당 쿠폰을 찾을 수 없습니다. id=$couponId")

        if (coupon.isUsed) {
            throw ConflictException("COUPON_ALREADY_USED", "이미 사용한 쿠폰이에요.")
        }
        if (coupon.isExpired) {
            throw BadRequestException("COUPON_EXPIRED", "만료된 쿠폰이에요.")
        }

        coupon.markUsed()
        return coupon.discountAmount
    }
}
