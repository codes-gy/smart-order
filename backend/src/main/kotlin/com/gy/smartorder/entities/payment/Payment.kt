package com.gy.smartorder.entities.payment

import com.gy.smartorder.entities.order.Order
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 결제 승인 기록. 주문(Order) 1건당 승인된 결제는 1건으로 본다(재결제/부분결제는 범위 밖).
 * 아직 PG(토스페이먼츠 등) 서버 연동 전 단계라, 클라이언트가 PG SDK로 결제를 완료한 뒤
 * 넘겨주는 paymentKey/amount를 서버가 주문 금액과 대조해 저장하는 최소 버전이다.
 * TODO(PG 실연동 시): paymentKey로 PG 결제 승인 API를 서버 side에서 재검증하는 로직 추가 필요.
 */
@Entity
@Table(
    name = "payment",
    indexes = [
        Index(name = "idx_payment_order_id", columnList = "order_id", unique = true),
        Index(name = "idx_payment_payment_key", columnList = "payment_key", unique = true),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Payment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    var order: Order,

    /** PG사가 발급하는 결제 건 식별자. 동일 키로 재요청이 와도 중복 승인되지 않도록 유니크 제약을 건다. */
    @Column(name = "payment_key", nullable = false, unique = true, length = 200)
    var paymentKey: String,

    @Column(nullable = false)
    var amount: Int,

    @CreatedDate
    @Column(name = "approved_at", nullable = false, updatable = false)
    var approvedAt: LocalDateTime = LocalDateTime.now(),
)
