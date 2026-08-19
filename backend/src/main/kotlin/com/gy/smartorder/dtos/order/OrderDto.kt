package com.gy.smartorder.dtos.order

import com.gy.smartorder.entities.order.Order
import com.gy.smartorder.entities.order.OrderItem
import com.gy.smartorder.entities.order.OrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime


class OrderDto {
    data class OrderItemCreateRequest(
        @field:NotNull(message = "메뉴 ID는 필수입니다.")
        val menuId: Long?,

        @field:NotNull(message = "수량은 필수입니다.")
        @field:Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
        val quantity: Int?,
    )

    data class OrderCreateRequest(
        @field:NotNull(message = "매장 ID는 필수입니다.")
        val storeId: Long?,

        @field:NotEmpty(message = "최소 하나 이상의 메뉴를 주문해야 합니다.")
        @field:Valid
        val items: List<OrderItemCreateRequest>,
    )

    data class OrderStatusUpdateRequest(
        @field:NotNull(message = "변경할 주문 상태는 필수입니다.")
        val status: OrderStatus?,
    )

    data class OrderItemResponse(
        val id: Long,
        val menuId: Long,
        val menuName: String,
        val price: Int,
        val quantity: Int,
        val totalPrice: Int,
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemResponse = OrderItemResponse(
                id = item.id,
                menuId = item.menuId,
                menuName = item.menuName,
                price = item.price,
                quantity = item.quantity,
                totalPrice = item.totalPrice,
            )
        }
    }

    data class OrderResponse(
        val id: Long,
        val storeId: Long,
        val totalPrice: Int,
        val status: OrderStatus,
        val items: List<OrderItemResponse>,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderResponse = OrderResponse(
                id = order.id,
                storeId = order.store.id,
                totalPrice = order.totalPrice,
                status = order.status,
                items = order.orderItems.map { OrderItemResponse.from(it) },
                createdAt = order.createdAt,
                updatedAt = order.updatedAt,
            )
        }
    }
}