package com.gy.smartorder.entities.order

import com.gy.smartorder.entities.store.Store
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

enum class OrderStatus {
    PENDING,    // 주문 접수 대기
    ACCEPTED,   // 주문 수락 (조리 중)
    READY,      // 조리 완료 (픽업 대기)
    COMPLETED,  // 픽업 완료
    CANCELLED   // 주문 취소
}

@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_orders_store_status", columnList = "store_id, status"),
        Index(name = "idx_orders_created_at", columnList = "created_at")
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

    @Column(nullable = false)
    var totalPrice: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING,

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