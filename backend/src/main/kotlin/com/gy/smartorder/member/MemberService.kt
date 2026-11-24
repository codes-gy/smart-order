package com.gy.smartorder.member

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.auth.AuthDto
import com.gy.smartorder.coupon.CouponDto
import com.gy.smartorder.coupon.CouponService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val couponService: CouponService,
) {
    companion object {
        private const val STAMP_GOAL = 10
        // 프론트 `utils/constants.ts`의 STAMP_REWARD_DISCOUNT와 동일 (스탬프 리워드는 정액 할인).
        private const val STAMP_REWARD_DISCOUNT = 4500
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

    /** 주문이 픽업 완료(PICKED_UP) 상태가 될 때 1개 적립된다. */
    @Transactional
    fun earnStamp(memberId: Long) {
        // 엔티티 += 1 대신 DB에서 원자적으로 더해, 동시에 픽업 완료된 주문 두 건의 적립이 하나로 합쳐지지 않게 한다.
        val updated = memberRepository.incrementStamp(memberId, LocalDateTime.now())
        if (updated == 0) {
            throw NotFoundException("MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다.")
        }
    }

    /**
     * 주문 생성 시 스탬프 리워드를 소비하고 할인액을 반환한다. 목표치(10개)를 채우지 못했으면 거부한다.
     * 동시 요청 레이스로 그새 다른 요청이 먼저 리셋했을 수도 있는 상태 변화라 Coupon의 "이미 사용됨"과
     * 동일하게 ConflictException으로 던져, 호출부(OrderService)가 멱등 재조회로 처리할 수 있게 한다.
     */
    @Transactional
    fun redeemStamp(memberId: Long): Int {
        val member = getMember(memberId)
        if (member.stampCount < STAMP_GOAL) {
            throw ConflictException("STAMP_NOT_ENOUGH", "스탬프가 아직 다 모이지 않았어요.")
        }
        // 위 검사는 빠른 실패용. 실제 소비는 "목표치 이상일 때만 0으로" 조건부 UPDATE로 원자적으로 처리해,
        // 동시 요청 두 건이 같은 리워드를 둘 다 쓰지 못하게 한다.
        val updated = memberRepository.resetStampIfReached(memberId, STAMP_GOAL, LocalDateTime.now())
        if (updated == 0) {
            throw ConflictException("STAMP_NOT_ENOUGH", "스탬프가 아직 다 모이지 않았어요.")
        }
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
