package com.gy.smartorder.repositories.order

import com.gy.smartorder.entities.order.Order
import com.gy.smartorder.entities.order.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByStoreIdOrderByCreatedAtDesc(storeId: Long): List<Order>
    fun findByStoreIdAndStatusOrderByCreatedAtDesc(storeId: Long, status: OrderStatus): List<Order>

    /** 멱등성 키(X-Idempotency-Key) 조회. 동일 키로 재요청 시 새 주문을 만들지 않고 이 결과를 재사용한다. */
    fun findByIdempotencyKey(idempotencyKey: String): Order?
}
