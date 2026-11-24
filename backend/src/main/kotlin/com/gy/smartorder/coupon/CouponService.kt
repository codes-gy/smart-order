package com.gy.smartorder.coupon

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
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

        // 위 검사는 빠른 실패/에러 구분용일 뿐, 실제 소비는 조건부 UPDATE 한 번으로 원자적으로 처리한다.
        // 동시에 같은 쿠폰을 쓰려는 요청이 있으면 한쪽만 1행을 갱신하고 다른 쪽은 0행 → 이미 사용됨.
        val updated = couponRepository.markUsedIfAvailable(couponId, memberId, LocalDateTime.now())
        if (updated == 0) {
            throw ConflictException("COUPON_ALREADY_USED", "이미 사용한 쿠폰이에요.")
        }
        return coupon.discountAmount
    }
}
