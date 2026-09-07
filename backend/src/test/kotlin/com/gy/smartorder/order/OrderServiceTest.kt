package com.gy.smartorder.order

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.category.Category
import com.gy.smartorder.menu.Menu
import com.gy.smartorder.menu.MenuStatus
import com.gy.smartorder.store.Store
import com.gy.smartorder.store.StoreStatus
import com.gy.smartorder.menu.MenuRepository
import com.gy.smartorder.store.StoreRepository
import com.gy.smartorder.coupon.CouponService
import com.gy.smartorder.member.MemberService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalTime

/**
 * OrderService의 쿠폰 할인 반영 로직(coupon 도메인 연동분)만 다루는 단위 테스트.
 * 그 외 OrderService 동작(재고 검증, 옵션 가격 등)은 이번 변경 범위 밖이라 커버하지 않는다.
 */
class OrderServiceTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var orderEventPublisher: OrderEventPublisher
    private lateinit var couponService: CouponService
    private lateinit var memberService: MemberService
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderRepository = mock(OrderRepository::class.java)
        storeRepository = mock(StoreRepository::class.java)
        menuRepository = mock(MenuRepository::class.java)
        orderEventPublisher = mock(OrderEventPublisher::class.java)
        couponService = mock(CouponService::class.java)
        memberService = mock(MemberService::class.java)
        orderService = OrderService(orderRepository, storeRepository, menuRepository, orderEventPublisher, couponService, memberService)
    }

    private fun store() = Store(
        id = 1L,
        name = "스마트오더 역삼역점",
        address = "서울시 강남구",
        phone = "02-0000-0000",
        status = StoreStatus.OPEN,
        businessNumber = "000-00-00000",
        latitude = 37.5,
        longitude = 127.0,
        openTime = LocalTime.of(9, 0),
        closeTime = LocalTime.of(22, 0),
    )

    private fun menu(store: Store, price: Int = 4000) = Menu(
        id = 1L,
        category = Category(id = 1L, store = store, name = "커피"),
        name = "아메리카노",
        price = price,
        status = MenuStatus.ON_SALE,
    )

    private fun createRequest(couponId: Long? = null, useStamp: Boolean = false) = OrderDto.OrderCreateRequest(
        storeId = 1L,
        items = listOf(OrderDto.OrderItemCreateRequest(menuId = 1L, quantity = 1)),
        packagingType = PackagingType.TAKE_OUT,
        couponId = couponId,
        useStamp = useStamp,
        idempotencyKey = "idem-key-1",
    )

    @Test
    fun `쿠폰 없이 주문하면 아이템 합계 그대로 총액이 된다`() {
        val store = store()
        given(orderRepository.findByIdempotencyKey("idem-key-1")).willReturn(null)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store)))
        given(orderRepository.save(any(Order::class.java))).willAnswer { it.arguments[0] as Order }

        val response = orderService.createOrder(createRequest(), null, memberId = 1L)

        assertThat(response.totalAmount).isEqualTo(4000)
        verify(couponService, never()).redeem(anyLong(), anyLong())
    }

    @Test
    fun `쿠폰을 지정하면 소비하고 할인액만큼 총액에서 뺀다`() {
        val store = store()
        given(orderRepository.findByIdempotencyKey("idem-key-1")).willReturn(null)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store)))
        given(couponService.redeem(1L, 10L)).willReturn(1000)
        given(orderRepository.save(any(Order::class.java))).willAnswer { it.arguments[0] as Order }

        val response = orderService.createOrder(createRequest(couponId = 10L), null, memberId = 1L)

        assertThat(response.totalAmount).isEqualTo(3000)
        verify(couponService).redeem(1L, 10L)
    }

    @Test
    fun `할인액이 아이템 합계보다 크면 총액은 0원 아래로 내려가지 않는다`() {
        val store = store()
        given(orderRepository.findByIdempotencyKey("idem-key-1")).willReturn(null)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store, price = 500)))
        given(couponService.redeem(1L, 10L)).willReturn(2000)
        given(orderRepository.save(any(Order::class.java))).willAnswer { it.arguments[0] as Order }

        val response = orderService.createOrder(createRequest(couponId = 10L), null, memberId = 1L)

        assertThat(response.totalAmount).isEqualTo(0)
    }

    @Test
    fun `유효하지 않은 쿠폰이면 CouponService의 예외가 그대로 전파되고 주문은 저장되지 않는다`() {
        val store = store()
        given(orderRepository.findByIdempotencyKey("idem-key-1")).willReturn(null)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store)))
        given(couponService.redeem(1L, 999L)).willThrow(NotFoundException("COUPON_NOT_FOUND", "해당 쿠폰을 찾을 수 없습니다."))

        assertThatThrownBy { orderService.createOrder(createRequest(couponId = 999L), null, memberId = 1L) }
            .isInstanceOf(NotFoundException::class.java)
        verify(orderRepository, never()).save(any(Order::class.java))
    }

    @Test
    fun `스탬프 리워드를 사용하면 소비하고 할인액만큼 총액에서 뺀다`() {
        val store = store()
        given(orderRepository.findByIdempotencyKey("idem-key-1")).willReturn(null)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store)))
        given(memberService.redeemStamp(1L)).willReturn(4500)
        given(orderRepository.save(any(Order::class.java))).willAnswer { it.arguments[0] as Order }

        val response = orderService.createOrder(createRequest(useStamp = true), null, memberId = 1L)

        assertThat(response.totalAmount).isEqualTo(0)
        verify(memberService).redeemStamp(1L)
    }

    @Test
    fun `동일 idempotencyKey의 동시 요청이 먼저 스탬프를 소비하고 주문을 완료했다면 그 주문을 멱등하게 반환한다`() {
        val store = store()
        val concurrentlyCreatedOrder = Order(
            id = 43L,
            store = store,
            memberId = 1L,
            totalPrice = 0,
            packagingType = PackagingType.TAKE_OUT,
            idempotencyKey = "idem-key-1",
        )
        given(orderRepository.findByIdempotencyKey("idem-key-1"))
            .willReturn(null, concurrentlyCreatedOrder)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store)))
        given(memberService.redeemStamp(1L))
            .willThrow(ConflictException("STAMP_NOT_ENOUGH", "스탬프가 아직 다 모이지 않았어요."))

        val response = orderService.createOrder(createRequest(useStamp = true), null, memberId = 1L)

        assertThat(response.orderId).isEqualTo("43")
        verify(orderRepository, never()).save(any(Order::class.java))
    }

    @Test
    fun `동일 idempotencyKey의 동시 요청이 먼저 쿠폰을 소비하고 주문을 완료했다면 그 주문을 멱등하게 반환한다`() {
        val store = store()
        val concurrentlyCreatedOrder = Order(
            id = 42L,
            store = store,
            memberId = 1L,
            totalPrice = 3000,
            packagingType = PackagingType.TAKE_OUT,
            idempotencyKey = "idem-key-1",
        )
        // 첫 조회(멱등성 체크)에는 아직 없다가, 쿠폰 redeem이 COUPON_ALREADY_USED로 실패한 뒤 재조회하면
        // 동시 요청이 그새 커밋한 주문이 보이는 상황을 재현한다.
        given(orderRepository.findByIdempotencyKey("idem-key-1"))
            .willReturn(null, concurrentlyCreatedOrder)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store)))
        given(couponService.redeem(1L, 10L))
            .willThrow(ConflictException("COUPON_ALREADY_USED", "이미 사용한 쿠폰이에요."))

        val response = orderService.createOrder(createRequest(couponId = 10L), null, memberId = 1L)

        assertThat(response.orderId).isEqualTo("42")
        assertThat(response.totalAmount).isEqualTo(3000)
        verify(orderRepository, never()).save(any(Order::class.java))
    }

    @Test
    fun `쿠폰이 진짜로 이미 사용됐고 동시 요청도 아니라면 ConflictException이 그대로 전파된다`() {
        val store = store()
        given(orderRepository.findByIdempotencyKey("idem-key-1")).willReturn(null)
        given(storeRepository.findById(1L)).willReturn(java.util.Optional.of(store))
        given(menuRepository.findById(1L)).willReturn(java.util.Optional.of(menu(store)))
        given(couponService.redeem(1L, 10L))
            .willThrow(ConflictException("COUPON_ALREADY_USED", "이미 사용한 쿠폰이에요."))

        assertThatThrownBy { orderService.createOrder(createRequest(couponId = 10L), null, memberId = 1L) }
            .isInstanceOf(ConflictException::class.java)
        verify(orderRepository, never()).save(any(Order::class.java))
    }

    private fun existingOrder(store: Store, status: com.gy.smartorder.order.OrderStatus) = Order(
        id = 100L,
        store = store,
        memberId = 1L,
        totalPrice = 4000,
        status = status,
        packagingType = PackagingType.TAKE_OUT,
        idempotencyKey = "idem-key-100",
    )

    @Test
    fun `주문이 PICKED_UP 상태로 바뀌면 스탬프를 1개 적립한다`() {
        val order = existingOrder(store(), status = com.gy.smartorder.order.OrderStatus.READY)
        given(orderRepository.findById(100L)).willReturn(java.util.Optional.of(order))

        orderService.updateOrderStatus(
            100L,
            OrderDto.OrderStatusUpdateRequest(status = com.gy.smartorder.order.OrderStatus.PICKED_UP),
        )

        verify(memberService).earnStamp(1L)
    }

    @Test
    fun `이미 PICKED_UP 상태인 주문을 다시 PICKED_UP으로 갱신해도 중복 적립하지 않는다`() {
        val order = existingOrder(store(), status = com.gy.smartorder.order.OrderStatus.PICKED_UP)
        given(orderRepository.findById(100L)).willReturn(java.util.Optional.of(order))

        orderService.updateOrderStatus(
            100L,
            OrderDto.OrderStatusUpdateRequest(status = com.gy.smartorder.order.OrderStatus.PICKED_UP),
        )

        verify(memberService, never()).earnStamp(anyLong())
    }

    @Test
    fun `PICKED_UP이 아닌 다른 상태로 바뀌면 적립하지 않는다`() {
        val order = existingOrder(store(), status = com.gy.smartorder.order.OrderStatus.ACCEPTED)
        given(orderRepository.findById(100L)).willReturn(java.util.Optional.of(order))

        orderService.updateOrderStatus(
            100L,
            OrderDto.OrderStatusUpdateRequest(status = com.gy.smartorder.order.OrderStatus.PREPARING),
        )

        verify(memberService, never()).earnStamp(anyLong())
    }
}
