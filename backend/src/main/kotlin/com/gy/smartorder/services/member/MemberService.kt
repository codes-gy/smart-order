package com.gy.smartorder.services.member

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.auth.AuthDto
import com.gy.smartorder.dtos.coupon.CouponDto
import com.gy.smartorder.dtos.member.MemberDto
import com.gy.smartorder.entities.member.Member
import com.gy.smartorder.entities.member.MemberStatus
import com.gy.smartorder.repositories.member.MemberRepository
import com.gy.smartorder.services.coupon.CouponService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val couponService: CouponService,
) {
    companion object {
        // 프론트 utils/constants.ts의 STAMP_REWARD_DISCOUNT, rewardsMock의 stampGoal(10)과 값을 맞춘다.
        const val STAMP_GOAL = 10
        const val STAMP_REWARD_DISCOUNT = 4500
    }

    fun getMe(memberId: Long): MemberDto.MeResponse {
        val member = getMember(memberId)
        return MemberDto.MeResponse(
            user = AuthDto.AuthUserResponse.from(member, isGuest = member.isGuest),
            rewards = getRewards(memberId),
        )
    }

    fun getRewards(memberId: Long): MemberDto.RewardsSummaryResponse {
        val member = getMember(memberId)
        return MemberDto.RewardsSummaryResponse(
            stampCount = member.stampCount,
            stampGoal = STAMP_GOAL,
            availableCouponCount = couponService.getAvailableCoupons(memberId).size,
        )
    }

    /** 결제 승인(PaymentService.confirmPayment) 시 1회 적립. 취소/미결제 주문은 적립되지 않는다. */
    @Transactional
    fun earnStamp(memberId: Long) {
        getMember(memberId).addStamp()
    }

    /**
     * 주문 생성 시 스탬프 리워드를 소비하고 할인액을 반환한다. 목표치 미달이면 거부한다.
     * `ConflictException`으로 던져 OrderService가 쿠폰과 동일한 방식(동시 요청 시 기존 주문 재조회)으로 처리할 수 있게 한다.
     */
    @Transactional
    fun redeemStampReward(memberId: Long): Int {
        val member = getMember(memberId)
        if (member.stampCount < STAMP_GOAL) {
            throw ConflictException("STAMP_NOT_ENOUGH", "스탬프가 부족해요.")
        }
        member.useStampReward(STAMP_GOAL)
        return STAMP_REWARD_DISCOUNT
    }

    // 즐겨찾기 도메인 미구현 상태라 항상 빈 배열 반환 (PROGRESS.md 4절 6번 항목).
    fun getFavorites(memberId: Long): List<MemberDto.FavoriteStoreResponse> {
        getMember(memberId)
        return emptyList()
    }

    fun getCoupons(memberId: Long): List<CouponDto.CouponResponse> {
        getMember(memberId)
        return couponService.getAvailableCoupons(memberId)
    }

    @Transactional
    fun updateProfile(memberId: Long, request: MemberDto.UpdateProfileRequest): AuthDto.AuthUserResponse {
        val member = getMember(memberId)
        member.updateProfile(request.nickname, request.phoneNumber)
        return AuthDto.AuthUserResponse.from(member, isGuest = member.isGuest)
    }

    @Transactional
    fun deleteAccount(memberId: Long) {
        val member = getMember(memberId)
        member.updateStatus(MemberStatus.INACTIVE)
    }

    private fun getMember(memberId: Long): Member =
        memberRepository.findById(memberId).orElseThrow {
            NotFoundException("MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다.")
        }
}
