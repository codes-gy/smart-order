package com.gy.smartorder.member

import com.gy.smartorder.member.Member
import com.gy.smartorder.member.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?
    fun findBySocialProviderAndSocialId(socialProvider: SocialProvider, socialId: String): Member?
    fun findByPhoneNumber(phoneNumber: String): Member?

    /**
     * 스탬프 1개 적립. 엔티티에서 `stampCount += 1`을 하면 동시 적립 두 건이 같은 값을 읽고 덮어써
     * 하나가 사라질 수 있어(lost update), DB에서 원자적으로 더한다. 벌크 UPDATE는 @LastModifiedDate를
     * 거치지 않으므로 수정 시각도 직접 넣는다.
     *
     * @return 갱신된 행 수 (0이면 회원 없음)
     */
    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.stampCount = m.stampCount + 1, m.modifiedDate = :now where m.id = :id")
    fun incrementStamp(@Param("id") id: Long, @Param("now") now: LocalDateTime): Int

    /**
     * 스탬프가 목표치 이상일 때만 0으로 초기화하는 조건부 UPDATE. 동시에 두 주문이 같은 리워드를
     * 쓰려 해도 정확히 한쪽만 1행을 갱신한다.
     *
     * @return 갱신된 행 수 (0이면 스탬프 부족 — 또는 그새 다른 요청이 먼저 사용함)
     */
    @Modifying(flushAutomatically = true)
    @Query(
        """
        update Member m set m.stampCount = 0, m.modifiedDate = :now
        where m.id = :id and m.stampCount >= :goal
        """
    )
    fun resetStampIfReached(@Param("id") id: Long, @Param("goal") goal: Int, @Param("now") now: LocalDateTime): Int
}
