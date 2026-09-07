package com.gy.smartorder.services.member

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.entities.member.Member
import com.gy.smartorder.repositories.member.MemberRepository
import com.gy.smartorder.services.coupon.CouponService
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

    private fun member(stampCount: Int = 0) = Member(
        id = 1L,
        nickname = "테스터",
        stampCount = stampCount,
    )

    @Test
    fun `결제 승인 시 스탬프를 1개 적립한다`() {
        val target = member(stampCount = 3)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))

        memberService.earnStamp(1L)

        assertThat(target.stampCount).isEqualTo(4)
    }

    @Test
    fun `목표치를 채운 상태에서 스탬프를 사용하면 목표치만큼 차감하고 할인액을 반환한다`() {
        val target = member(stampCount = 10)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))

        val discount = memberService.redeemStampReward(1L)

        assertThat(discount).isEqualTo(MemberService.STAMP_REWARD_DISCOUNT)
        assertThat(target.stampCount).isEqualTo(0)
    }

    @Test
    fun `목표치보다 많이 모았어도 목표치만큼만 차감한다`() {
        val target = member(stampCount = 13)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))

        memberService.redeemStampReward(1L)

        assertThat(target.stampCount).isEqualTo(3)
    }

    @Test
    fun `목표치 미달이면 ConflictException을 던지고 차감하지 않는다`() {
        val target = member(stampCount = 9)
        given(memberRepository.findById(1L)).willReturn(Optional.of(target))

        assertThatThrownBy { memberService.redeemStampReward(1L) }
            .isInstanceOf(ConflictException::class.java)
        assertThat(target.stampCount).isEqualTo(9)
    }

    @Test
    fun `존재하지 않는 회원이면 NotFoundException을 던진다`() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy { memberService.earnStamp(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }
}
