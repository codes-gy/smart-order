package com.gy.smartorder.repositories.payment

import com.gy.smartorder.entities.payment.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): Payment?

    /** 멱등 처리용: 같은 PG 결제 키로 재요청이 오면 새로 만들지 않고 이 결과를 재사용한다. */
    fun findByPaymentKey(paymentKey: String): Payment?
}
