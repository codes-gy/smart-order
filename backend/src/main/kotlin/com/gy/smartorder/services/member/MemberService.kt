package com.gy.smartorder.services.member

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

    fun getMe(memberId: Long): MemberDto.MeResponse {
        val member = getMember(memberId)
        return MemberDto.MeResponse(
            user = AuthDto.AuthUserResponse.from(member, isGuest = member.isGuest),
            rewards = getRewards(memberId),
        )
    }

    fun getRewards(memberId: Long): MemberDto.RewardsSummaryResponse {
        getMember(memberId)
        // stampCount/stampGoal은 Member 적립 도메인 미구현 상태라 0값 placeholder(PROGRESS.md 참고).
        return MemberDto.RewardsSummaryResponse(
            stampCount = 0,
            stampGoal = 10,
            availableCouponCount = couponService.getAvailableCoupons(memberId).size,
        )
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
