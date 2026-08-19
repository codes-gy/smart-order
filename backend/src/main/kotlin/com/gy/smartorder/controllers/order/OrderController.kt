package com.gy.smartorder.controllers.order

import com.gy.smartorder.dtos.order.OrderDto
import com.gy.smartorder.entities.order.OrderStatus
import com.gy.smartorder.services.order.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
)
{
    @PostMapping
    fun createOrder(
        @Valid @RequestBody req: OrderDto.OrderCreateRequest,
    ): ResponseEntity<OrderDto.OrderResponse> {
        val res = orderService.createOrder(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: Long,
    ): ResponseEntity<OrderDto.OrderResponse> {
        val res = orderService.getOrder(orderId)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/store/{storeId}")
    fun getOrdersByStore(
        @PathVariable storeId: Long,
        @RequestParam(required = false) status: OrderStatus?,
    ): ResponseEntity<List<OrderDto.OrderResponse>> {
        val res = orderService.getOrdersByStore(storeId, status)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @Valid @RequestBody req: OrderDto.OrderStatusUpdateRequest,
    ): ResponseEntity<OrderDto.OrderResponse> {
        val res = orderService.updateOrderStatus(orderId, req)
        return ResponseEntity.ok(res)
    }
}