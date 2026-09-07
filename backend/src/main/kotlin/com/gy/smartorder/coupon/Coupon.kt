package com.gy.smartorder.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 회원에게 발급된 쿠폰 1장. 발급 시점의 이름/할인액을 그대로 스냅샷으로 들고 있어(Order/OrderItem과
 * 동일한 스냅샷 컨벤션), 나중에 쿠폰 정책이 바뀌어도 이미 발급된 쿠폰의 값은 그대로 유지된다.
 * 프론트 `Coupon`({ id, name, discountAmount, expiresAt })과 1:1.
 */
@Entity
@Table(
    name = "coupons",
    indexes = [Index(name = "idx_coupons_member_id", columnList = "member_id")],
)
@EntityListeners(AuditingEntityListener::class)
class Coupon(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "member_id", nullable = false)
    var memberId: Long,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Int,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    /** null이면 아직 사용하지 않은 쿠폰. 사용 시점에 채워지며, 한 번 채워지면 다시 null로 되돌리지 않는다. */
    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val isUsed: Boolean
        get() = usedAt != null

    val isExpired: Boolean
        get() = expiresAt.isBefore(LocalDateTime.now())

    fun markUsed() {
        usedAt = LocalDateTime.now()
    }
}
