package com.gy.smartorder.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByStoreIdOrderByCreatedAtDesc(storeId: Long): List<Order>
    fun findByStoreIdAndStatusOrderByCreatedAtDesc(storeId: Long, status: OrderStatus): List<Order>

    /**
     * 멱등성 키(X-Idempotency-Key) 조회. 동일 회원이 동일 키로 재요청 시 새 주문을 만들지 않고 이 결과를 재사용한다.
     * 키는 회원 단위로만 유일하므로 반드시 memberId와 함께 조회한다 (다른 회원의 주문이 반환되면 안 된다).
     */
    fun findByMemberIdAndIdempotencyKey(memberId: Long, idempotencyKey: String): Order?
}
