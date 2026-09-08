package com.gy.smartorder.services.payment

import com.gy.smartorder.dtos.payment.PaymentDto
import com.gy.smartorder.entities.order.Order
import com.gy.smartorder.entities.order.PackagingType
import com.gy.smartorder.entities.payment.Payment
import com.gy.smartorder.entities.store.Store
import com.gy.smartorder.entities.store.StoreStatus
import com.gy.smartorder.repositories.order.OrderRepository
import com.gy.smartorder.repositories.payment.PaymentRepository
import com.gy.smartorder.services.member.MemberService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalTime

/**
 * PaymentService의 스탬프 적립 연동분(member 도메인 연동)만 다루는 단위 테스트.
 * 그 외 PaymentService 동작(금액 검증, 멱등 처리 등)은 이번 변경 범위 밖이라 커버하지 않는다.
 */
class PaymentServiceTest {

    private lateinit var paymentRepository: PaymentRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var memberService: MemberService
    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setUp() {
        paymentRepository = mock(PaymentRepository::class.java)
        orderRepository = mock(OrderRepository::class.java)
        memberService = mock(MemberService::class.java)
        paymentService = PaymentService(paymentRepository, orderRepository, memberService)
    }

    private fun order() = Order(
        id = 1L,
        store = Store(
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
        ),
        memberId = 7L,
        totalPrice = 4000,
        packagingType = PackagingType.TAKE_OUT,
        idempotencyKey = "idem-key-1",
    )

    private fun confirmRequest() = PaymentDto.PaymentConfirmRequest(
        orderId = 1L,
        paymentKey = "payment-key-1",
        amount = 4000,
    )

    @Test
    fun `결제가 새로 승인되면 주문한 회원에게 스탬프를 적립한다`() {
        given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(null)
        given(orderRepository.findById(1L)).willReturn(java.util.Optional.of(order()))
        given(paymentRepository.findByOrderId(1L)).willReturn(null)
        given(paymentRepository.save(any(Payment::class.java))).willAnswer { it.arguments[0] as Payment }

        paymentService.confirmPayment(confirmRequest())

        verify(memberService).earnStamp(7L)
    }

    @Test
    fun `이미 승인된 결제 키로 재요청하면 스탬프를 다시 적립하지 않는다`() {
        val existing = Payment(order = order(), paymentKey = "payment-key-1", amount = 4000)
        given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(existing)

        paymentService.confirmPayment(confirmRequest())

        verify(memberService, never()).earnStamp(org.mockito.ArgumentMatchers.anyLong())
    }

    @Test
    fun `같은 주문에 이미 승인된 결제가 있으면 스탬프를 다시 적립하지 않는다`() {
        given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(null)
        given(orderRepository.findById(1L)).willReturn(java.util.Optional.of(order()))
        given(paymentRepository.findByOrderId(1L))
            .willReturn(Payment(order = order(), paymentKey = "other-key", amount = 4000))

        paymentService.confirmPayment(confirmRequest())

        verify(memberService, never()).earnStamp(org.mockito.ArgumentMatchers.anyLong())
    }
}
