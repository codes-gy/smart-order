package com.gy.smartorder.services.payment

import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.payment.PaymentDto
import com.gy.smartorder.entities.payment.Payment
import com.gy.smartorder.repositories.order.OrderRepository
import com.gy.smartorder.repositories.payment.PaymentRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun confirmPayment(req: PaymentDto.PaymentConfirmRequest): PaymentDto.PaymentConfirmResponse {
        val paymentKey = req.paymentKey!!

        // 같은 결제 키로 이미 승인 처리된 결제가 있으면 새로 만들지 않고 그대로 재반환한다 (클라이언트 재시도 대응).
        paymentRepository.findByPaymentKey(paymentKey)?.let { existing ->
            return PaymentDto.PaymentConfirmResponse.from(existing)
        }

        val orderId = req.orderId!!
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw NotFoundException("ORDER_NOT_FOUND", "해당 주문을 찾을 수 없습니다. id=$orderId")

        // 같은 주문에 대해 다른 paymentKey로 이미 승인된 결제가 있으면(예: 응답 유실 후 재시도) 기존 결제를 반환한다.
        paymentRepository.findByOrderId(orderId)?.let { existing ->
            return PaymentDto.PaymentConfirmResponse.from(existing)
        }

        val amount = req.amount!!
        if (amount != order.totalPrice) {
            throw ConflictException(
                code = "PAYMENT_AMOUNT_MISMATCH",
                message = "결제 금액이 주문 금액과 일치하지 않습니다.",
                details = mapOf(
                    "expected" to listOf(order.totalPrice.toString()),
                    "actual" to listOf(amount.toString()),
                ),
            )
        }

        val payment = Payment(
            order = order,
            paymentKey = paymentKey,
            amount = amount,
        )

        val savedPayment = try {
            paymentRepository.save(payment)
        } catch (ex: DataIntegrityViolationException) {
            // 동시에 같은 주문/결제 키로 요청이 들어온 경쟁 상태: 새로 만들지 않고 기존 결제를 찾아 반환한다.
            paymentRepository.findByPaymentKey(paymentKey)
                ?: paymentRepository.findByOrderId(orderId)
                ?: throw ex
        }

        return PaymentDto.PaymentConfirmResponse.from(savedPayment)
    }
}
