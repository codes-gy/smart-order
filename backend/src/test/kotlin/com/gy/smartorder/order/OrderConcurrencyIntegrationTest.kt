package com.gy.smartorder.order

import com.gy.smartorder.category.Category
import com.gy.smartorder.category.CategoryRepository
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.coupon.Coupon
import com.gy.smartorder.coupon.CouponRepository
import com.gy.smartorder.coupon.CouponService
import com.gy.smartorder.member.Member
import com.gy.smartorder.member.MemberRepository
import com.gy.smartorder.member.MemberService
import com.gy.smartorder.menu.Menu
import com.gy.smartorder.menu.MenuRepository
import com.gy.smartorder.menu.MenuStatus
import com.gy.smartorder.store.Store
import com.gy.smartorder.store.StoreRepository
import com.gy.smartorder.store.StoreStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 실제 DB(H2, test 프로필)와 실제 트랜잭션 프록시를 거쳐 동시 요청 경쟁을 재현하는 통합 테스트.
 * OrderServiceTest의 목(mock) 기반 레이스 시뮬레이션은 "예외가 나면 재조회한다"는 분기만 확인할 뿐,
 * rollback-only로 인한 UnexpectedRollbackException이나 read-then-write 이중 사용은 잡지 못한다.
 *
 * 모든 스레드를 CountDownLatch로 출발선에 세운 뒤 한 번에 풀어 최대한 동시에 부딪히게 한다.
 */
@SpringBootTest(properties = ["management.health.redis.enabled=false"])
@ActiveProfiles("test")
class OrderConcurrencyIntegrationTest(
    @Autowired private val orderService: OrderService,
    @Autowired private val couponService: CouponService,
    @Autowired private val memberService: MemberService,
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val couponRepository: CouponRepository,
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val storeRepository: StoreRepository,
    @Autowired private val categoryRepository: CategoryRepository,
    @Autowired private val menuRepository: MenuRepository,
) {
    companion object {
        private const val THREADS = 16
        private const val MENU_PRICE = 4000
        private const val COUPON_DISCOUNT = 2000
    }

    private lateinit var store: Store
    private lateinit var category: Category
    private lateinit var menu: Menu
    private val memberIds = mutableListOf<Long>()
    private val couponIds = mutableListOf<Long>()

    @BeforeEach
    fun setUp() {
        store = storeRepository.save(
            Store(
                name = "동시성 테스트 매장",
                address = "서울시 강남구",
                phone = "02-0000-0000",
                status = StoreStatus.OPEN,
                businessNumber = UUID.randomUUID().toString().take(20),
                latitude = 37.5,
                longitude = 127.0,
                openTime = LocalTime.of(0, 0),
                closeTime = LocalTime.of(23, 59),
            )
        )
        category = categoryRepository.save(Category(store = store, name = "커피-${UUID.randomUUID()}".take(30)))
        menu = menuRepository.save(Menu(category = category, name = "아메리카노", price = MENU_PRICE, status = MenuStatus.ON_SALE))
    }

    @AfterEach
    fun tearDown() {
        memberIds.forEach { memberId ->
            orderRepository.deleteAll(orderRepository.findAll().filter { it.memberId == memberId })
        }
        couponRepository.deleteAllById(couponIds)
        memberRepository.deleteAllById(memberIds)
        menuRepository.delete(menu)
        categoryRepository.delete(category)
        storeRepository.delete(store)
        memberIds.clear()
        couponIds.clear()
    }

    private fun newMember(stampCount: Int = 0): Long =
        memberRepository.save(Member(nickname = "동시성테스터", stampCount = stampCount)).id!!.also { memberIds += it }

    private fun newCoupon(memberId: Long): Long =
        couponRepository.save(
            Coupon(
                memberId = memberId,
                name = "테스트 쿠폰",
                discountAmount = COUPON_DISCOUNT,
                expiresAt = LocalDateTime.now().plusDays(1),
            )
        ).id.also { couponIds += it }

    private fun orderRequest(couponId: Long? = null, useStamp: Boolean = false) = OrderDto.OrderCreateRequest(
        storeId = store.id,
        items = listOf(OrderDto.OrderItemCreateRequest(menuId = menu.id, quantity = 1)),
        packagingType = PackagingType.TAKE_OUT,
        couponId = couponId,
        useStamp = useStamp,
    )

    /** n개 작업을 동시에 출발시키고 각 결과(성공 값 또는 예외)를 모은다. */
    private fun <T> runConcurrently(n: Int, task: (Int) -> T): List<Result<T>> {
        val pool = Executors.newFixedThreadPool(n)
        val ready = CountDownLatch(n)
        val start = CountDownLatch(1)
        try {
            val futures = (0 until n).map { i ->
                pool.submit<Result<T>> {
                    ready.countDown()
                    start.await()
                    runCatching { task(i) }
                }
            }
            ready.await(10, TimeUnit.SECONDS)
            start.countDown()
            return futures.map { it.get(60, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
    }

    private fun ordersOf(memberId: Long): List<Order> = orderRepository.findAll().filter { it.memberId == memberId }

    @Test
    fun `같은 쿠폰을 동시에 여러 번 사용해도 정확히 한 번만 성공한다`() {
        val memberId = newMember()
        val couponId = newCoupon(memberId)

        val results = runConcurrently(THREADS) { couponService.redeem(memberId, couponId) }

        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.filter { it.isFailure }.map { it.exceptionOrNull() })
            .allSatisfy { assertThat(it).isInstanceOf(ConflictException::class.java) }
        assertThat(couponRepository.findById(couponId).get().isUsed).isTrue()
    }

    @Test
    fun `스탬프 리워드를 동시에 여러 번 사용해도 정확히 한 번만 성공한다`() {
        val memberId = newMember(stampCount = 10)

        val results = runConcurrently(THREADS) { memberService.redeemStamp(memberId) }

        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.filter { it.isFailure }.map { it.exceptionOrNull() })
            .allSatisfy { assertThat(it).isInstanceOf(ConflictException::class.java) }
        assertThat(memberRepository.findById(memberId).get().stampCount).isEqualTo(0)
    }

    @Test
    fun `스탬프 적립이 동시에 몰려도 하나도 유실되지 않는다`() {
        val memberId = newMember(stampCount = 0)

        val results = runConcurrently(THREADS) { memberService.earnStamp(memberId) }

        assertThat(results).allSatisfy { assertThat(it.isSuccess).isTrue() }
        assertThat(memberRepository.findById(memberId).get().stampCount).isEqualTo(THREADS)
    }

    @Test
    fun `같은 회원이 같은 멱등성 키로 쿠폰 주문을 동시에 보내면 주문 1건만 생기고 모두 같은 주문을 돌려받는다`() {
        val memberId = newMember()
        val couponId = newCoupon(memberId)
        val key = "idem-${UUID.randomUUID()}"

        val results = runConcurrently(THREADS) { orderService.createOrder(orderRequest(couponId = couponId), key, memberId) }

        // 늦은 요청은 쿠폰 조건부 UPDATE에서 충돌 → 트랜잭션 롤백 → 트랜잭션 밖에서 재조회로 멱등 반환.
        // (예전 구조였다면 같은 트랜잭션에서 예외를 삼켜 UnexpectedRollbackException이 났다.)
        assertThat(results).allSatisfy { assertThat(it.exceptionOrNull()).isNull() }
        val orders = ordersOf(memberId)
        assertThat(orders).hasSize(1)
        assertThat(results.map { it.getOrThrow().orderId }.toSet()).containsExactly(orders.single().id.toString())
        assertThat(orders.single().totalPrice).isEqualTo(MENU_PRICE - COUPON_DISCOUNT)
        assertThat(couponRepository.findById(couponId).get().isUsed).isTrue()
    }

    @Test
    fun `같은 회원이 같은 멱등성 키로 쿠폰 없이 동시에 주문해도 유니크 제약으로 주문은 1건만 생긴다`() {
        val memberId = newMember()
        val key = "idem-${UUID.randomUUID()}"

        val results = runConcurrently(THREADS) { orderService.createOrder(orderRequest(), key, memberId) }

        assertThat(results).allSatisfy { assertThat(it.exceptionOrNull()).isNull() }
        val orders = ordersOf(memberId)
        assertThat(orders).hasSize(1)
        assertThat(results.map { it.getOrThrow().orderId }.toSet()).containsExactly(orders.single().id.toString())
    }

    @Test
    fun `다른 회원이 우연히 같은 멱등성 키를 써도 각자 자기 주문을 받는다`() {
        val memberA = newMember()
        val memberB = newMember()
        val key = "idem-${UUID.randomUUID()}"

        val resultA = orderService.createOrder(orderRequest(), key, memberA)
        val resultB = orderService.createOrder(orderRequest(), key, memberB)

        assertThat(resultB.orderId).isNotEqualTo(resultA.orderId)
        assertThat(ordersOf(memberA).single().id.toString()).isEqualTo(resultA.orderId)
        assertThat(ordersOf(memberB).single().id.toString()).isEqualTo(resultB.orderId)
    }
}
