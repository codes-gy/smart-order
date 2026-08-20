package com.gy.smartorder.services.order

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.order.OrderDto
import com.gy.smartorder.entities.menu.MenuStatus
import com.gy.smartorder.entities.order.Order
import com.gy.smartorder.entities.order.OrderItem
import com.gy.smartorder.entities.order.OrderStatus
import com.gy.smartorder.repositories.menu.MenuRepository
import com.gy.smartorder.repositories.order.OrderRepository
import com.gy.smartorder.repositories.store.StoreRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val storeRepository: StoreRepository,
    private val menuRepository: MenuRepository,
    private val orderEventPublisher: OrderEventPublisher,
)
{
    /** 주문 생성 직전 재고/판매상태를 서버 기준으로 재검증한다 (PRD 3.3, F-03). */
    fun validateOrder(req: OrderDto.OrderValidateRequest): OrderDto.OrderValidateResponse {
        val issues = validateItems(req.items)
        return OrderDto.OrderValidateResponse(isValid = issues.isEmpty(), issues = issues)
    }

    @Transactional
    fun createOrder(req: OrderDto.OrderCreateRequest, idempotencyKeyHeader: String?): OrderDto.OrderCreateResponse {
        val idempotencyKey = idempotencyKeyHeader?.takeIf { it.isNotBlank() }
            ?: req.idempotencyKey?.takeIf { it.isNotBlank() }
            ?: throw BadRequestException(
                "IDEMPOTENCY_KEY_REQUIRED",
                "멱등성 키(X-Idempotency-Key)가 필요합니다.",
            )

        // 같은 키로 이미 만들어진 주문이 있으면 새로 만들지 않고 그대로 재반환한다 (진짜 멱등 동작).
        orderRepository.findByIdempotencyKey(idempotencyKey)?.let { existing ->
            return OrderDto.OrderCreateResponse(orderId = existing.id.toString(), totalAmount = existing.totalPrice)
        }

        val storeId = req.storeId!!
        val store = storeRepository.findByIdOrNull(storeId)
            ?: throw NotFoundException("STORE_NOT_FOUND", "해당 매장을 찾을 수 없습니다. id=$storeId")

        val issues = validateItems(req.items)
        if (issues.isNotEmpty()) {
            throw ConflictException(
                code = "ORDER_VALIDATION_FAILED",
                message = "주문 내용이 변경되었어요. 장바구니를 다시 확인해주세요.",
                details = mapOf("issues" to issues.map { it.message }),
            )
        }

        val order = Order(
            store = store,
            packagingType = req.packagingType!!,
            couponId = req.couponId,
            useStamp = req.useStamp,
            idempotencyKey = idempotencyKey,
        )

        req.items.forEach { itemReq ->
            val menu = menuRepository.findByIdOrNull(itemReq.menuId!!)
                ?: throw NotFoundException("MENU_NOT_FOUND", "해당 메뉴를 찾을 수 없습니다. id=${itemReq.menuId}")
            val quantity = itemReq.quantity!!
            val orderItem = OrderItem(
                menuId = menu.id,
                menuName = menu.name,
                price = menu.price,
                quantity = quantity,
                totalPrice = menu.price * quantity,
                optionChoiceIds = itemReq.optionChoiceIds.toMutableList(),
            )
            order.addOrderItem(orderItem)
        }

        // coupon/member(적립) 도메인이 아직 없어 할인은 반영하지 않는다. 아이템 합계를 그대로 총액으로 쓴다.
        order.totalPrice = order.orderItems.sumOf { it.totalPrice }

        val savedOrder = try {
            orderRepository.save(order)
        } catch (ex: DataIntegrityViolationException) {
            // 동시에 같은 idempotencyKey로 요청이 들어온 경쟁 상태: 새로 만들지 않고 기존 주문을 찾아 반환한다.
            orderRepository.findByIdempotencyKey(idempotencyKey) ?: throw ex
        }

        return OrderDto.OrderCreateResponse(orderId = savedOrder.id.toString(), totalAmount = savedOrder.totalPrice)
    }

    fun getOrder(orderId: Long): OrderDto.OrderResponse {
        val order = findOrderOrThrow(orderId)
        return OrderDto.OrderResponse.from(order)
    }

    /** 매장 관리자 주문 큐/내역 (F-05). status 미지정 시 전체 내역을 반환한다. */
    fun getOrdersByStore(storeId: Long, status: OrderStatus?): List<OrderDto.OrderSummaryResponse> {
        val orders = if (status != null) {
            orderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status)
        } else {
            orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
        }
        return orders.map { OrderDto.OrderSummaryResponse.from(it) }
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, req: OrderDto.OrderStatusUpdateRequest): OrderDto.OrderResponse {
        val order = findOrderOrThrow(orderId)
        order.updateStatus(req.status!!)
        // @LastModifiedDate(updatedAt)를 지금 시점에 반영해, 뒤이어 만드는 SSE 이벤트의 시각이 정확하게 한다.
        orderRepository.flush()
        orderEventPublisher.publish(orderId, OrderDto.OrderTrackingEvent.from(order))
        return OrderDto.OrderResponse.from(order)
    }

    /**
     * 주문 상태 실시간 추적 SSE 구독 (PRD 4.1, F-04).
     * 구독 시점의 현재 상태를 첫 이벤트로 즉시 보내, 이미 상태가 진행된 뒤 접속한 클라이언트도
     * 다음 상태 변경을 기다리지 않고 바로 현재 상태를 알 수 있게 한다.
     */
    fun subscribeToOrderEvents(orderId: Long): SseEmitter {
        val order = findOrderOrThrow(orderId)
        return orderEventPublisher.subscribe(orderId, OrderDto.OrderTrackingEvent.from(order))
    }

    private fun validateItems(items: List<OrderDto.OrderItemCreateRequest>): List<OrderDto.OrderIssue> {
        val issues = mutableListOf<OrderDto.OrderIssue>()
        items.forEach { itemReq ->
            val menuId = itemReq.menuId!!
            val menu = menuRepository.findByIdOrNull(menuId)
            if (menu == null) {
                issues.add(
                    OrderDto.OrderIssue(
                        menuId = menuId.toString(),
                        menuName = menuId.toString(),
                        reason = OrderDto.OrderIssueReason.SOLD_OUT,
                        message = "더 이상 판매하지 않는 메뉴예요.",
                    )
                )
                return@forEach
            }
            if (menu.status == MenuStatus.SOLD_OUT) {
                issues.add(
                    OrderDto.OrderIssue(
                        menuId = menu.id.toString(),
                        menuName = menu.name,
                        reason = OrderDto.OrderIssueReason.SOLD_OUT,
                        message = "${menu.name}이(가) 품절되었어요.",
                    )
                )
            }
            // NOTE: 메뉴 옵션 그룹 도메인이 아직 없어 OPTION_SOLD_OUT은 판정하지 못한다.
            // PRICE_CHANGED도 프론트가 라인별 기대 가격을 보내지 않아(현재 계약상) 비교 대상이 없다.
        }
        return issues
    }

    private fun findOrderOrThrow(orderId: Long): Order =
        orderRepository.findByIdOrNull(orderId)
            ?: throw NotFoundException("ORDER_NOT_FOUND", "해당 주문을 찾을 수 없습니다. id=$orderId")
}
