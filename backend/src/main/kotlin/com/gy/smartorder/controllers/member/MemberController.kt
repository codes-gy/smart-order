package com.gy.smartorder.controllers.member

import com.gy.smartorder.dtos.auth.AuthDto
import com.gy.smartorder.dtos.member.MemberDto
import com.gy.smartorder.services.member.MemberService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/members")
class MemberController(
    private val memberService: MemberService,
) {

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal memberId: Long): ResponseEntity<MemberDto.MeResponse> {
        return ResponseEntity.ok(memberService.getMe(memberId))
    }

    @PatchMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody req: MemberDto.UpdateProfileRequest,
    ): ResponseEntity<AuthDto.AuthUserResponse> {
        return ResponseEntity.ok(memberService.updateProfile(memberId, req))
    }

    @DeleteMapping("/me")
    fun deleteAccount(@AuthenticationPrincipal memberId: Long): ResponseEntity<Void> {
        memberService.deleteAccount(memberId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/rewards")
    fun getRewards(@AuthenticationPrincipal memberId: Long): ResponseEntity<MemberDto.RewardsSummaryResponse> {
        return ResponseEntity.ok(memberService.getRewards(memberId))
    }

    @GetMapping("/favorites")
    fun getFavorites(@AuthenticationPrincipal memberId: Long): ResponseEntity<List<MemberDto.FavoriteStoreResponse>> {
        return ResponseEntity.ok(memberService.getFavorites(memberId))
    }

    @GetMapping("/coupons")
    fun getCoupons(@AuthenticationPrincipal memberId: Long): ResponseEntity<List<MemberDto.CouponResponse>> {
        return ResponseEntity.ok(memberService.getCoupons(memberId))
    }
}
