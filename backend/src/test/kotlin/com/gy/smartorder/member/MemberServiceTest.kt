package com.gy.smartorder.member

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.coupon.CouponService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import java.util.Optional

class MemberServiceTest {

    private lateinit var memberRepository: MemberRepository
    private lateinit var couponService: CouponService
    private lateinit var memberService: MemberService

    @BeforeEach
    fun setUp() {
        memberRepository = mock(MemberRepository::class.java)
        couponService = mock(CouponService::class.java)
        memberService = MemberService(memberRepository, couponService)
    }

    /** CouponServiceTest와 같은 이유: Kotlin non-null 파라미터에 Mockito any()의 null이 들어가지 않게 우회. */
    private fun anyLocalDateTime(): LocalDateTime {
        any(LocalDateTime::class.java)
        return LocalDateTime.now()
    }

    private fun member(id: Long = 1L, stampCount: Int = 0) = Member(
        id = id,
        nickname = "테스터",
        stampCount = stampCount,
    )

    @Test
    fun `주문이 픽업 완료되면 스탬프가 1개 늘어난다`() {
        given(memberRepository.incrementStamp(eq(1L), anyLocalDateTime())).willReturn(1)

        memberService.earnStamp(1L)

        verify(memberRepository).incrementStamp(eq(1L), anyLocalDateTime())
    }

    @Test
    fun `적립 대상 회원이 없으면 NotFoundException을 던진다`() {
        given(memberRepository.incrementStamp(eq(1L), anyLocalDateTime())).willReturn(0)

        assertThatThrownBy { memberService.earnStamp(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `스탬프가 목표치를 채웠으면 소비하고 고정 할인액을 반환한다`() {
        val target = member(stampCount = 10)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))
        given(memberRepository.resetStampIfReached(eq(1L), eq(10), anyLocalDateTime())).willReturn(1)

        val discount = memberService.redeemStamp(1L)

        assertThat(discount).isEqualTo(4500)
        verify(memberRepository).resetStampIfReached(eq(1L), eq(10), anyLocalDateTime())
    }

    @Test
    fun `조회 시점엔 목표치였지만 조건부 UPDATE가 0행이면(동시 사용) ConflictException을 던진다`() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(stampCount = 10)))
        given(memberRepository.resetStampIfReached(eq(1L), eq(10), anyLocalDateTime())).willReturn(0)

        assertThatThrownBy { memberService.redeemStamp(1L) }
            .isInstanceOf(ConflictException::class.java)
    }

    @Test
    fun `스탬프가 목표치보다 부족하면 ConflictException을 던진다`() {
        val target = member(stampCount = 9)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))

        assertThatThrownBy { memberService.redeemStamp(1L) }
            .isInstanceOf(ConflictException::class.java)
        assertThat(target.stampCount).isEqualTo(9)
    }

    @Test
    fun `리워드 조회 시 실제 스탬프 개수와 목표치(10), 보유 쿠폰 수를 반환한다`() {
        val target = member(stampCount = 7)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))
        given(couponService.getAvailableCoupons(1L)).willReturn(emptyList())

        val rewards = memberService.getRewards(1L)

        assertThat(rewards.stampCount).isEqualTo(7)
        assertThat(rewards.stampGoal).isEqualTo(10)
        assertThat(rewards.availableCouponCount).isEqualTo(0)
    }
}
