package com.gy.smartorder.coupon

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDateTime

class CouponServiceTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var couponService: CouponService

    @BeforeEach
    fun setUp() {
        couponRepository = mock(CouponRepository::class.java)
        couponService = CouponService(couponRepository)
    }

    /**
     * Mockito의 `any(Class)`는 null을 반환하는데, Kotlin에서 선언한 리포지토리 메서드의 파라미터가
     * non-null 타입(`LocalDateTime`)이면 컴파일러가 호출부에 null 체크를 끼워 넣어 NPE가 난다.
     * matcher 등록은 그대로 하되 non-null 더미 값을 반환해 우회하는 통상적인 해법.
     */
    private fun anyLocalDateTime(): LocalDateTime {
        any(LocalDateTime::class.java)
        return LocalDateTime.now()
    }

    private fun coupon(
        id: Long = 1L,
        memberId: Long = 1L,
        discountAmount: Int = 2000,
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(30),
        usedAt: LocalDateTime? = null,
    ) = Coupon(
        id = id,
        memberId = memberId,
        name = "웰컴 2,000원 할인 쿠폰",
        discountAmount = discountAmount,
        expiresAt = expiresAt,
        usedAt = usedAt,
    )

    @Test
    fun `신규 회원 생성 시 웰컴 쿠폰을 발급한다`() {
        couponService.issueWelcomeCoupon(1L)

        verify(couponRepository).save(any(Coupon::class.java))
    }

    @Test
    fun `사용 가능한 쿠폰 목록을 조회한다`() {
        given(
            couponRepository.findByMemberIdAndUsedAtIsNullAndExpiresAtAfterOrderByExpiresAtAsc(
                eq(1L),
                anyLocalDateTime(),
            )
        ).willReturn(listOf(coupon()))

        val result = couponService.getAvailableCoupons(1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].discountAmount).isEqualTo(2000)
    }

    @Test
    fun `본인 소유의 유효한 쿠폰을 사용하면 할인액을 반환하고 사용 처리한다`() {
        val target = coupon()
        given(couponRepository.findByIdAndMemberId(1L, 1L)).willReturn(target)

        val discount = couponService.redeem(memberId = 1L, couponId = 1L)

        assertThat(discount).isEqualTo(2000)
        assertThat(target.isUsed).isTrue()
    }

    @Test
    fun `존재하지 않거나 본인 소유가 아닌 쿠폰이면 NotFoundException을 던진다`() {
        given(couponRepository.findByIdAndMemberId(1L, 1L)).willReturn(null)

        assertThatThrownBy { couponService.redeem(memberId = 1L, couponId = 1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `이미 사용한 쿠폰이면 ConflictException을 던진다`() {
        given(couponRepository.findByIdAndMemberId(1L, 1L))
            .willReturn(coupon(usedAt = LocalDateTime.now().minusDays(1)))

        assertThatThrownBy { couponService.redeem(memberId = 1L, couponId = 1L) }
            .isInstanceOf(ConflictException::class.java)
    }

    @Test
    fun `만료된 쿠폰이면 BadRequestException을 던진다`() {
        given(couponRepository.findByIdAndMemberId(1L, 1L))
            .willReturn(coupon(expiresAt = LocalDateTime.now().minusDays(1)))

        assertThatThrownBy { couponService.redeem(memberId = 1L, couponId = 1L) }
            .isInstanceOf(BadRequestException::class.java)
    }
}
