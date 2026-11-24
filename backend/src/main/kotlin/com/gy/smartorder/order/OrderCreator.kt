package com.gy.smartorder.order

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.coupon.CouponService
import com.gy.smartorder.member.MemberService
import com.gy.smartorder.menu.Menu
import com.gy.smartorder.menu.MenuRepository
import com.gy.smartorder.menu.MenuStatus
import com.gy.smartorder.store.StoreRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 1건을 "하나의 트랜잭션"으로 만든다: 재고 재검증 → 쿠폰/스탬프 소비 → 주문 INSERT.
 *
 * OrderService에서 분리한 이유: 쿠폰 이미 사용(ConflictException)이나 멱등성 키 유니크 위반
 * (DataIntegrityViolationException)이 나면 이 트랜잭션은 rollback-only가 된다. 같은 트랜잭션 안에서
 * 예외를 잡고 기존 주문을 재조회해 정상 반환하면 커밋 시점에 UnexpectedRollbackException이 터진다.
 * 그래서 트랜잭션 경계를 이 컴포넌트의 [create]로 좁히고, 예외 처리 + 멱등 재조회는 트랜잭션 밖
 * ([OrderService.createOrder])에서 한다. 트랜잭션이 통째로 롤백되므로 실패한 쪽이 쿠폰/스탬프를
 * 소비한 흔적도 남지 않는다.
 */
@Component
class OrderCreator(
    private val orderRepository: OrderRepository,
    private val storeRepository: StoreRepository,
    private val menuRepository: MenuRepository,
    private val couponService: CouponService,
    private val memberService: MemberService,
) {

    @Transactional
    fun create(req: OrderDto.OrderCreateRequest, idempotencyKey: String, memberId: Long): Order {
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
            memberId = memberId,
            packagingType = req.packagingType!!,
            couponId = req.couponId,
            useStamp = req.useStamp,
            idempotencyKey = idempotencyKey,
        )

        req.items.forEach { itemReq ->
            val menu = menuRepository.findByIdOrNull(itemReq.menuId!!)
                ?: throw NotFoundException("MENU_NOT_FOUND", "해당 메뉴를 찾을 수 없습니다. id=${itemReq.menuId}")
            val quantity = itemReq.quantity!!
            val unitPrice = menu.price + optionsPriceDelta(menu, itemReq.optionChoiceIds)
            val orderItem = OrderItem(
                menuId = menu.id,
                menuName = menu.name,
                price = unitPrice,
                quantity = quantity,
                totalPrice = unitPrice * quantity,
                optionChoiceIds = itemReq.optionChoiceIds.toMutableList(),
            )
            order.addOrderItem(orderItem)
        }

        val itemsAmount = order.orderItems.sumOf { it.totalPrice }
        // 여기서 나는 ConflictException(COUPON_ALREADY_USED / STAMP_NOT_ENOUGH)은 잡지 않는다 —
        // 동시 재요청 여부 판단은 트랜잭션이 롤백된 뒤 OrderService가 한다.
        val couponDiscount = req.couponId?.let { couponService.redeem(memberId, it) } ?: 0
        val stampDiscount = if (req.useStamp) memberService.redeemStamp(memberId) else 0
        order.totalPrice = (itemsAmount - couponDiscount - stampDiscount).coerceAtLeast(0)

        return orderRepository.save(order)
    }

    fun validateItems(items: List<OrderDto.OrderItemCreateRequest>): List<OrderDto.OrderIssue> {
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

            val soldOutChoice = menu.optionGroups
                .flatMap { it.choices }
                .find { choice -> choice.id in itemReq.optionChoiceIds && choice.isSoldOut }
            if (soldOutChoice != null) {
                issues.add(
                    OrderDto.OrderIssue(
                        menuId = menu.id.toString(),
                        menuName = menu.name,
                        reason = OrderDto.OrderIssueReason.OPTION_SOLD_OUT,
                        message = "${menu.name}의 '${soldOutChoice.label}' 옵션이 품절되었어요.",
                    )
                )
            }
            // NOTE: PRICE_CHANGED는 프론트가 라인별 기대 가격을 보내지 않아(현재 계약상) 비교 대상이 없다.
        }
        return issues
    }

    /** 요청한 옵션 choice ID들의 priceDelta 합. 메뉴에 실제로 없는 choice ID는 무시한다. */
    private fun optionsPriceDelta(menu: Menu, optionChoiceIds: List<Long>): Int =
        menu.optionGroups
            .flatMap { it.choices }
            .filter { choice -> choice.id in optionChoiceIds }
            .sumOf { it.priceDelta }
}
