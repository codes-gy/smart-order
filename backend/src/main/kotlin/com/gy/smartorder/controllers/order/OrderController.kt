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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
)
{
    /** 주문 생성 직전 재고/판매상태 재검증 (PRD 3.3, F-03). */
    @PostMapping("/validate")
    fun validateOrder(
        @Valid @RequestBody req: OrderDto.OrderValidateRequest,
    ): ResponseEntity<OrderDto.OrderValidateResponse> {
        val res = orderService.validateOrder(req)
        return ResponseEntity.ok(res)
    }

    /**
     * 주문 생성. 프로젝트 규칙(멱등성 키)에 따라 `X-Idempotency-Key` 헤더를 우선으로 멱등성을
     * 보장하며, 헤더가 없으면 바디의 idempotencyKey로 폴백한다. 둘 다 없으면 400.
     */
    @PostMapping
    fun createOrder(
        @RequestHeader(value = "X-Idempotency-Key", required = false) idempotencyKeyHeader: String?,
        @Valid @RequestBody req: OrderDto.OrderCreateRequest,
    ): ResponseEntity<OrderDto.OrderCreateResponse> {
        val res = orderService.createOrder(req, idempotencyKeyHeader)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: Long,
    ): ResponseEntity<OrderDto.OrderResponse> {
        val res = orderService.getOrder(orderId)
        return ResponseEntity.ok(res)
    }

    /** 매장 관리자 주문 큐/내역 (F-05). status 미지정 시 전체 내역을 반환한다. */
    @GetMapping("/store/{storeId}")
    fun getOrdersByStore(
        @PathVariable storeId: Long,
        @RequestParam(required = false) status: OrderStatus?,
    ): ResponseEntity<List<OrderDto.OrderSummaryResponse>> {
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
