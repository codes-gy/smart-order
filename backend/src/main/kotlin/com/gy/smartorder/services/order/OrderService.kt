package com.gy.smartorder.services.order

import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.order.OrderDto
import com.gy.smartorder.entities.order.Order
import com.gy.smartorder.entities.order.OrderItem
import com.gy.smartorder.entities.order.OrderStatus
import com.gy.smartorder.repositories.menu.MenuRepository
import com.gy.smartorder.repositories.order.OrderRepository
import com.gy.smartorder.repositories.store.StoreRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val storeRepository: StoreRepository,
    private val menuRepository: MenuRepository,
)
{
    @Transactional
    fun createOrder(req: OrderDto.OrderCreateRequest): OrderDto.OrderResponse {
        val storeId = req.storeId!!
        val store = storeRepository.findByIdOrNull(storeId)
            ?: throw NotFoundException("STORE_NOT_FOUND", "해당 매장을 찾을 수 없습니다. id=$storeId")

        val order = Order(store = store)

        req.items.forEach { itemReq ->
            val menuId = itemReq.menuId!!
            val menu = menuRepository.findByIdOrNull(menuId)
                ?: throw NotFoundException("MENU_NOT_FOUND", "해당 메뉴를 찾을 수 없습니다. id=$menuId")

            val quantity = itemReq.quantity!!
            val orderItem = OrderItem(
                menuId = menu.id,
                menuName = menu.name,
                price = menu.price,
                quantity = quantity,
                totalPrice = menu.price * quantity,
            )
            order.addOrderItem(orderItem)
        }

        order.calculateTotalPrice()
        val savedOrder = orderRepository.save(order)
        return OrderDto.OrderResponse.from(savedOrder)
    }

    fun getOrder(orderId: Long): OrderDto.OrderResponse {
        val order = findOrderOrThrow(orderId)
        return OrderDto.OrderResponse.from(order)
    }

    fun getOrdersByStore(storeId: Long, status: OrderStatus?): List<OrderDto.OrderResponse> {
        val orders = if (status != null) {
            orderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status)
        } else {
            orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
        }
        return orders.map { OrderDto.OrderResponse.from(it) }
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, req: OrderDto.OrderStatusUpdateRequest): OrderDto.OrderResponse {
        val order = findOrderOrThrow(orderId)
        order.updateStatus(req.status!!)
        return OrderDto.OrderResponse.from(order)
    }

    private fun findOrderOrThrow(orderId: Long): Order =
        orderRepository.findByIdOrNull(orderId)
            ?: throw NotFoundException("ORDER_NOT_FOUND", "해당 주문을 찾을 수 없습니다. id=$orderId")
}