package com.gy.smartorder.order

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.ForbiddenException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.member.MemberService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * 클래스 레벨 @Transactional을 두지 않는다 — [createOrder]는 트랜잭션 "밖"에서 예외를 받아 멱등 재조회를
 * 해야 하기 때문이다. (클래스 레벨 readOnly + 메서드 NOT_SUPPORTED 조합은 트랜잭션 동기화가 켜진 채로
 * 남아, 트랜잭션 없는 조회가 묶어 둔 EntityManager가 커넥션을 메서드 끝까지 쥐고 있게 된다. 그 상태로
 * 내부 트랜잭션이 커넥션을 하나 더 잡으면 동시 요청이 몰릴 때 커넥션 풀이 고갈돼 교착된다.)
 * 나머지 메서드는 각자 트랜잭션을 명시한다.
 */
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderCreator: OrderCreator,
    private val orderEventPublisher: OrderEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberService: MemberService,
)
{
    /** 주문 생성 직전 재고/판매상태를 서버 기준으로 재검증한다 (PRD 3.3, F-03). */
    @Transactional(readOnly = true)
    fun validateOrder(req: OrderDto.OrderValidateRequest): OrderDto.OrderValidateResponse {
        val issues = orderCreator.validateItems(req.items)
        return OrderDto.OrderValidateResponse(isValid = issues.isEmpty(), issues = issues)
    }

    /**
     * 트랜잭션은 [OrderCreator.create] 안에서만 연다 — 이 메서드 자체는 트랜잭션 밖이다.
     * 같은 회원·같은 멱등성 키로 동시에 요청이 두 개 오면, 늦은 쪽은 쿠폰/스탬프 조건부 UPDATE에서
     * ConflictException을 받거나 (member_id, idempotency_key) 유니크 위반을 받는다. 이때 늦은 쪽의
     * 트랜잭션은 이미 통째로 롤백된 상태이고, 여기(트랜잭션 밖)서 먼저 커밋된 주문을 재조회해 멱등하게 반환한다.
     */
    fun createOrder(
        req: OrderDto.OrderCreateRequest,
        idempotencyKeyHeader: String?,
        memberId: Long,
    ): OrderDto.OrderCreateResponse {
        val idempotencyKey = idempotencyKeyHeader?.takeIf { it.isNotBlank() }
            ?: req.idempotencyKey?.takeIf { it.isNotBlank() }
            ?: throw BadRequestException(
                "IDEMPOTENCY_KEY_REQUIRED",
                "멱등성 키(X-Idempotency-Key)가 필요합니다.",
            )

        // 같은 회원이 같은 키로 이미 만든 주문이 있으면 새로 만들지 않고 그대로 재반환한다 (진짜 멱등 동작).
        findExisting(memberId, idempotencyKey)?.let { return it }

        val savedOrder = try {
            orderCreator.create(req, idempotencyKey, memberId)
        } catch (ex: ConflictException) {
            // 동일 키의 동시 요청이 먼저 쿠폰/스탬프를 소비하고 주문까지 커밋했다면 COUPON_ALREADY_USED /
            // STAMP_NOT_ENOUGH 대신 그 주문을 반환한다. 아니라면 진짜 충돌이므로 그대로 던진다.
            findExisting(memberId, idempotencyKey)?.let { return it }
            throw ex
        } catch (ex: DataIntegrityViolationException) {
            // (member_id, idempotency_key) 유니크 위반: 동시 요청이 먼저 INSERT를 커밋한 경쟁 상태.
            findExisting(memberId, idempotencyKey)?.let { return it }
            throw ex
        }

        return OrderDto.OrderCreateResponse(orderId = savedOrder.id.toString(), totalAmount = savedOrder.totalPrice)
    }

    /**
     * @param principalId 요청자 식별자 — Member 토큰이면 memberId, STORE_ADMIN 토큰이면 storeId
     *   (JwtAuthenticationFilter가 그렇게 principal을 채운다. AuthController.logout()의 동일 패턴 참고).
     * @param isStoreAdmin true면 order.store.id와, false면 order.memberId와 비교해 소유권을 검증한다.
     */
    @Transactional(readOnly = true)
    fun getOrder(orderId: Long, principalId: Long, isStoreAdmin: Boolean): OrderDto.OrderResponse {
        val order = findOrderOrThrow(orderId)
        assertOrderAccess(order, principalId, isStoreAdmin)
        return OrderDto.OrderResponse.from(order)
    }

    /** 매장 관리자 주문 큐/내역 (F-05). status 미지정 시 전체 내역을 반환한다. */
    @Transactional(readOnly = true)
    fun getOrdersByStore(storeId: Long, status: OrderStatus?, principalStoreId: Long): List<OrderDto.OrderSummaryResponse> {
        assertOwnStore(storeId, principalStoreId)
        val orders = if (status != null) {
            orderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status)
        } else {
            orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
        }
        return orders.map { OrderDto.OrderSummaryResponse.from(it) }
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, req: OrderDto.OrderStatusUpdateRequest, principalStoreId: Long): OrderDto.OrderResponse {
        val order = findOrderOrThrow(orderId)
        assertOwnStore(order.store.id, principalStoreId)
        // 이전 상태가 이미 PICKED_UP이면(상태 업데이트 재요청 등) 중복 적립하지 않는다.
        val justPickedUp = order.status != OrderStatus.PICKED_UP && req.status == OrderStatus.PICKED_UP
        order.updateStatus(req.status!!)
        if (justPickedUp) {
            memberService.earnStamp(order.memberId)
        }
        // @LastModifiedDate(updatedAt)를 지금 시점에 반영해, 뒤이어 만드는 SSE 이벤트의 시각이 정확하게 한다.
        orderRepository.flush()
        // SSE 전송은 커밋 이후에만 일어나도록 이벤트로 넘긴다(OrderEventPublisher의 AFTER_COMMIT 리스너).
        // 여기서 바로 보내면 뒤이어 커밋이 실패했을 때 클라이언트는 실제로 저장되지 않은 상태를 보게 된다.
        applicationEventPublisher.publishEvent(OrderStatusChangedEvent(orderId, OrderDto.OrderTrackingEvent.from(order)))
        return OrderDto.OrderResponse.from(order)
    }

    /**
     * 주문 상태 실시간 추적 SSE 구독 (PRD 4.1, F-04).
     * 구독 시점의 현재 상태를 첫 이벤트로 즉시 보내, 이미 상태가 진행된 뒤 접속한 클라이언트도
     * 다음 상태 변경을 기다리지 않고 바로 현재 상태를 알 수 있게 한다.
     */
    @Transactional(readOnly = true)
    fun subscribeToOrderEvents(orderId: Long, principalId: Long, isStoreAdmin: Boolean): SseEmitter {
        val order = findOrderOrThrow(orderId)
        assertOrderAccess(order, principalId, isStoreAdmin)
        return orderEventPublisher.subscribe(orderId, OrderDto.OrderTrackingEvent.from(order))
    }

    private fun findExisting(memberId: Long, idempotencyKey: String): OrderDto.OrderCreateResponse? =
        orderRepository.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey)?.let { existing ->
            OrderDto.OrderCreateResponse(orderId = existing.id.toString(), totalAmount = existing.totalPrice)
        }

    private fun findOrderOrThrow(orderId: Long): Order =
        orderRepository.findByIdOrNull(orderId)
            ?: throw NotFoundException("ORDER_NOT_FOUND", "해당 주문을 찾을 수 없습니다. id=$orderId")

    private fun assertOrderAccess(order: Order, principalId: Long, isStoreAdmin: Boolean) {
        val authorized = if (isStoreAdmin) order.store.id == principalId else order.memberId == principalId
        if (!authorized) {
            throw ForbiddenException("ORDER_ACCESS_DENIED", "본인 주문 또는 소속 매장의 주문만 조회할 수 있습니다.")
        }
    }

    private fun assertOwnStore(storeId: Long, principalStoreId: Long) {
        if (storeId != principalStoreId) {
            throw ForbiddenException("STORE_ACCESS_DENIED", "다른 매장의 주문에는 접근할 수 없습니다.")
        }
    }
}
