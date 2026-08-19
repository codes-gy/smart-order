package com.gy.smartorder.repositories.order

import com.gy.smartorder.entities.order.Order
import com.gy.smartorder.entities.order.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByStoreIdOrderByCreatedAtDesc(storeId: Long): List<Order>
    fun findByStoreIdAndStatusOrderByCreatedAtDesc(storeId: Long, status: OrderStatus): List<Order>
}