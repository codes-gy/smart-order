package com.gy.smartorder.order

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime


class OrderDto {

    /** 검증 실패 사유. 프론트 `ValidateOrderIssue.reason`과 값을 맞춘다. */
    enum class OrderIssueReason {
        SOLD_OUT,
        OPTION_SOLD_OUT,
        PRICE_CHANGED,
    }

    data class OrderItemCreateRequest(
        @field:NotNull(message = "메뉴 ID는 필수입니다.")
        val menuId: Long?,

        @field:NotNull(message = "수량은 필수입니다.")
        @field:Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
        val quantity: Int?,

        /** 선택한 옵션 choice ID 목록. 옵션이 없는 메뉴는 빈 배열을 보낸다. */
        val optionChoiceIds: List<Long> = emptyList(),
    )

    /** 주문 생성 직전, 서버 기준 재고/판매상태 재검증 요청 (PRD 3.3, F-03). */
    data class OrderValidateRequest(
        @field:NotNull(message = "매장 ID는 필수입니다.")
        val storeId: Long?,

        @field:NotEmpty(message = "최소 하나 이상의 메뉴를 주문해야 합니다.")
        @field:Valid
        val items: List<OrderItemCreateRequest>,
    )

    data class OrderIssue(
        val menuId: String,
        val menuName: String,
        val reason: OrderIssueReason,
        val message: String,
    )

    data class OrderValidateResponse(
        val isValid: Boolean,
        val issues: List<OrderIssue>,
    )

    data class OrderCreateRequest(
        @field:NotNull(message = "매장 ID는 필수입니다.")
        val storeId: Long?,

        @field:NotEmpty(message = "최소 하나 이상의 메뉴를 주문해야 합니다.")
        @field:Valid
        val items: List<OrderItemCreateRequest>,

        @field:NotNull(message = "포장 방식은 필수입니다.")
        val packagingType: PackagingType?,

        /** 적용할 쿠폰 ID. `OrderService.createOrder()`가 `CouponService.redeem()`으로 소유/유효성 검증 후 할인에 반영한다. */
        val couponId: Long? = null,

        /** member(적립) 도메인 구현 전까지는 값만 저장하고 할인 계산에는 반영하지 않는다. */
        val useStamp: Boolean = false,

        /**
         * 클라이언트 생성 멱등성 키. `X-Idempotency-Key` 헤더를 우선으로 사용하되,
         * 프론트 `CreateOrderRequest` 타입이 바디에도 이 필드를 정의해두고 있어
         * 헤더가 없을 때의 폴백으로 바디값도 허용한다 (컨트롤러에서 헤더 우선 병합).
         */
        val idempotencyKey: String? = null,
    )

    data class OrderCreateResponse(
        val orderId: String,
        val totalAmount: Int,
    )

    data class OrderStatusUpdateRequest(
        @field:NotNull(message = "변경할 주문 상태는 필수입니다.")
        val status: OrderStatus?,
    )

    data class OrderItemResponse(
        val id: String,
        val menuId: String,
        val menuName: String,
        val price: Int,
        val quantity: Int,
        val totalPrice: Int,
        val optionChoiceIds: List<String>,
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemResponse = OrderItemResponse(
                id = item.id.toString(),
                menuId = item.menuId.toString(),
                menuName = item.menuName,
                price = item.price,
                quantity = item.quantity,
                totalPrice = item.totalPrice,
                optionChoiceIds = item.optionChoiceIds.map { it.toString() },
            )
        }
    }

    /** 주문 상세 응답. 매장 관리자 상세 화면 등 전체 필드가 필요한 곳에서 사용한다. */
    data class OrderResponse(
        val id: String,
        val storeId: String,
        val totalPrice: Int,
        val status: OrderStatus,
        val packagingType: PackagingType,
        val couponId: String?,
        val useStamp: Boolean,
        val items: List<OrderItemResponse>,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderResponse = OrderResponse(
                id = order.id.toString(),
                storeId = order.store.id.toString(),
                totalPrice = order.totalPrice,
                status = order.status,
                packagingType = order.packagingType,
                couponId = order.couponId?.toString(),
                useStamp = order.useStamp,
                items = order.orderItems.map { OrderItemResponse.from(it) },
                createdAt = order.createdAt,
                updatedAt = order.updatedAt,
            )
        }
    }

    /** 주문 내역 요약. 프론트 `OrderHistoryItem`과 1:1로 대응한다 (내 주문 내역 / 매장 주문 큐 F-04, F-05). */
    data class OrderSummaryResponse(
        val orderId: String,
        val storeId: String,
        val storeName: String,
        val status: OrderStatus,
        val totalAmount: Int,
        val itemsSummary: String,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderSummaryResponse = OrderSummaryResponse(
                orderId = order.id.toString(),
                storeId = order.store.id.toString(),
                storeName = order.store.name,
                status = order.status,
                totalAmount = order.totalPrice,
                itemsSummary = buildItemsSummary(order.orderItems),
                createdAt = order.createdAt,
            )

            private fun buildItemsSummary(items: List<OrderItem>): String {
                if (items.isEmpty()) return ""
                val first = items.first().menuName
                return if (items.size > 1) "$first 외 ${items.size - 1}건" else first
            }
        }
    }

    /**
     * SSE(또는 REST Polling 폴백)로 전달되는 주문 상태 갱신 이벤트.
     * 프론트 `OrderTrackingEvent`({ orderId, status, updatedAt, message })와 1:1로 대응한다.
     */
    data class OrderTrackingEvent(
        val orderId: String,
        val status: OrderStatus,
        val updatedAt: LocalDateTime,
        val message: String,
    ) {
        companion object {
            /** 프론트 orderTrackingMock.ts의 STATUS_MESSAGE와 문구를 맞춘다. */
            private val STATUS_MESSAGE: Map<OrderStatus, String> = mapOf(
                OrderStatus.PENDING to "주문을 접수하고 있어요",
                OrderStatus.ACCEPTED to "매장에서 주문을 확인했어요",
                OrderStatus.PREPARING to "음료를 제조하고 있어요",
                OrderStatus.READY to "픽업 준비가 완료됐어요",
                OrderStatus.PICKED_UP to "픽업이 완료됐어요",
                OrderStatus.CANCELLED to "주문이 취소됐어요",
            )

            fun from(order: Order): OrderTrackingEvent = OrderTrackingEvent(
                orderId = order.id.toString(),
                status = order.status,
                updatedAt = order.updatedAt,
                message = STATUS_MESSAGE[order.status] ?: "",
            )
        }
    }
}
