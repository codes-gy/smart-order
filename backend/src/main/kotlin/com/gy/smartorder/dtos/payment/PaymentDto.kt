package com.gy.smartorder.dtos.payment

import com.gy.smartorder.entities.payment.Payment
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

class PaymentDto {

    /** 프론트 `ConfirmPaymentRequest`({ orderId, paymentKey, amount })와 대응한다. */
    data class PaymentConfirmRequest(
        @field:NotNull(message = "주문 ID는 필수입니다.")
        val orderId: Long?,

        @field:NotBlank(message = "결제 키는 필수입니다.")
        val paymentKey: String?,

        @field:NotNull(message = "결제 금액은 필수입니다.")
        @field:Min(value = 0, message = "결제 금액은 0원 이상이어야 합니다.")
        val amount: Int?,
    )

    /** 프론트 `ConfirmPaymentResponse`({ orderId, approvedAt })와 대응한다. */
    data class PaymentConfirmResponse(
        val orderId: String,
        val approvedAt: LocalDateTime,
    ) {
        companion object {
            fun from(payment: Payment): PaymentConfirmResponse = PaymentConfirmResponse(
                orderId = payment.order.id.toString(),
                approvedAt = payment.approvedAt,
            )
        }
    }
}
