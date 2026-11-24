package com.gy.smartorder.order

/**
 * 주문 상태가 바뀌었음을 알리는 스프링 애플리케이션 이벤트.
 * [OrderEventPublisher]가 트랜잭션 커밋 이후(AFTER_COMMIT)에만 받아 SSE로 내보낸다 —
 * 커밋 전에 보내면 이후 롤백됐을 때 클라이언트가 실제로는 없는 상태를 보게 된다.
 */
data class OrderStatusChangedEvent(
    val orderId: Long,
    val payload: OrderDto.OrderTrackingEvent,
)
