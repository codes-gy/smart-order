package com.gy.smartorder.order

import com.gy.smartorder.store.Store
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 주문 상태 라이프사이클(PRD 4.4): PENDING -> ACCEPTED -> PREPARING -> READY -> PICKED_UP (또는 CANCELLED).
 * 프론트 `order.types.ts`의 OrderStatus 값과 1:1로 맞춘다 (이전에는 PREPARING이 없고 COMPLETED였음).
 */
enum class OrderStatus {
    PENDING,    // 주문 접수 대기
    ACCEPTED,   // 주문 수락
    PREPARING,  // 조리 중
    READY,      // 조리 완료 (픽업 대기)
    PICKED_UP,  // 픽업 완료
    CANCELLED   // 주문 취소
}

/** 포장 방식. 프론트 `cart.types.ts`의 PackagingType과 값을 맞춘다. */
enum class PackagingType {
    TAKE_OUT,
    DINE_IN,
}

@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_orders_store_status", columnList = "store_id, status"),
        Index(name = "idx_orders_created_at", columnList = "created_at"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Order(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    /** 주문한 회원 ID. JWT 인증 principal(`@AuthenticationPrincipal`)에서 그대로 가져온다. */
    @Column(name = "member_id", nullable = false)
    var memberId: Long,

    @Column(nullable = false)
    var totalPrice: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var packagingType: PackagingType = PackagingType.TAKE_OUT,

    /** 적용한 쿠폰 ID. 실제 할인 반영/소비 처리는 `OrderService.createOrder()`가 `CouponService.redeem()`으로 한다. */
    @Column(name = "coupon_id")
    var couponId: Long? = null,

    /** 적립(스탬프) 사용 여부. true면 `OrderService.createOrder()`가 `MemberService.redeemStamp()`로 소비하고 할인액을 totalPrice에 반영한다. */
    @Column(nullable = false)
    var useStamp: Boolean = false,

    /**
     * 클라이언트가 생성한 멱등성 키(X-Idempotency-Key). 유니크 제약으로 동시에 같은 키로
     * 재요청이 들어와도 주문이 두 번 생성되지 않도록 DB 레벨에서 보장한다.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    var idempotencyKey: String,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var orderItems: MutableList<OrderItem> = mutableListOf(),

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun addOrderItem(orderItem: OrderItem) {
        orderItems.add(orderItem)
        orderItem.order = this
    }

    fun updateStatus(status: OrderStatus) {
        this.status = status
    }

    fun calculateTotalPrice() {
        this.totalPrice = orderItems.sumOf { it.totalPrice }
    }
}
