package com.gy.smartorder.coupon

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface CouponRepository : JpaRepository<Coupon, Long> {
    fun findByMemberIdAndUsedAtIsNullAndExpiresAtAfterOrderByExpiresAtAsc(
        memberId: Long,
        now: LocalDateTime,
    ): List<Coupon>

    fun findByIdAndMemberId(id: Long, memberId: Long): Coupon?

    /**
     * 아직 쓰지 않았고 만료되지 않은 경우에만 사용 처리하는 조건부 UPDATE.
     * 읽고 나서 엔티티를 고치는(read-then-write) 방식은 동시 요청 두 개가 둘 다 "미사용"을 읽고
     * 둘 다 사용 처리할 수 있다. `used_at IS NULL` 조건을 UPDATE 자체에 넣으면 DB 행 잠금 덕분에
     * 정확히 한 요청만 1행을 갱신하고, 나머지는 0을 받는다.
     *
     * @return 갱신된 행 수 (1이면 이번 요청이 사용 처리에 성공, 0이면 그새 다른 요청이 먼저 사용함)
     */
    @Modifying(flushAutomatically = true)
    @Query(
        """
        update Coupon c set c.usedAt = :now
        where c.id = :id and c.memberId = :memberId and c.usedAt is null and c.expiresAt > :now
        """
    )
    fun markUsedIfAvailable(
        @Param("id") id: Long,
        @Param("memberId") memberId: Long,
        @Param("now") now: LocalDateTime,
    ): Int
}
