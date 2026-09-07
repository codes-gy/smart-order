package com.gy.smartorder.repositories.member

import com.gy.smartorder.entities.member.Member
import com.gy.smartorder.entities.member.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?
    fun findBySocialProviderAndSocialId(socialProvider: SocialProvider, socialId: String): Member?
    fun findByPhoneNumber(phoneNumber: String): Member?
}