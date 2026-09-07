package com.gy.smartorder.dtos.member

import com.gy.smartorder.dtos.auth.AuthDto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

class MemberDto {

    // stampCount/stampGoal은 Member 적립 도메인 미구현 상태라 0값 placeholder(후속 작업 대상).
    // availableCouponCount는 CouponService 실제 값을 반영한다.
    data class RewardsSummaryResponse(
        val stampCount: Int,
        val stampGoal: Int,
        val availableCouponCount: Int,
    )

    // frontend authApi.me()의 Promise<MeResponse>({ user, rewards })와 1:1
    data class MeResponse(
        val user: AuthDto.AuthUserResponse,
        val rewards: RewardsSummaryResponse,
    )

    // frontend FavoriteStore와 1:1. 즐겨찾기 도메인 미구현 상태라 지금은 항상 빈 배열로 내려준다.
    data class FavoriteStoreResponse(
        val storeId: String,
        val storeName: String,
        val lastOrderedAt: LocalDateTime,
    )

    data class UpdateProfileRequest(
        @field:NotBlank(message = "닉네임은 필수입니다.")
        @field:Size(min = 2, max = 50, message = "닉네임은 2~50자 사이여야 합니다.")
        val nickname: String,

        @field:Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        val phoneNumber: String? = null,
    )
}
