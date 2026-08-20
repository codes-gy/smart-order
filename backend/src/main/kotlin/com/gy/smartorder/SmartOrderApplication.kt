package com.gy.smartorder

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/** @EnableScheduling: 주문 SSE 하트비트(OrderEventPublisher.heartbeat, PRD 4.1)를 위해 필요하다. */
@EnableScheduling
@SpringBootApplication
class SmartOrderApplication

fun main(args: Array<String>) {
    runApplication<SmartOrderApplication>(*args)
}
