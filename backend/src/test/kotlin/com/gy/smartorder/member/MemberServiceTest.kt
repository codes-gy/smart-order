package com.gy.smartorder.member

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.coupon.CouponService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
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

    private fun member(id: Long = 1L, stampCount: Int = 0) = Member(
        id = id,
        nickname = "테스터",
        stampCount = stampCount,
    )

    @Test
    fun `주문이 픽업 완료되면 스탬프가 1개 늘어난다`() {
        val target = member(stampCount = 3)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))

        memberService.earnStamp(1L)

        assertThat(target.stampCount).isEqualTo(4)
    }

    @Test
    fun `스탬프가 목표치를 채웠으면 소비하고 고정 할인액을 반환한다`() {
        val target = member(stampCount = 10)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))

        val discount = memberService.redeemStamp(1L)

        assertThat(discount).isEqualTo(4500)
        assertThat(target.stampCount).isEqualTo(0)
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
