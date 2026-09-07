package com.gy.smartorder.payment

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {

    /** 결제 승인 확인 (PRD 6.B, 4.2). 클라이언트가 PG SDK로 결제를 마친 뒤 호출한다. */
    @PostMapping("/confirm")
    fun confirmPayment(
        @Valid @RequestBody req: PaymentDto.PaymentConfirmRequest,
    ): ResponseEntity<PaymentDto.PaymentConfirmResponse> {
        val res = paymentService.confirmPayment(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }
}
