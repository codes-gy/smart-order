package com.gy.smartorder.services.order

import com.gy.smartorder.dtos.order.OrderDto
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 주문 단위 SSE 구독자 레지스트리 + 브로드캐스터 (프로젝트 지침 4절 SSE 규약, PRD 4.1).
 *
 * - subscribe(): 새 구독을 등록하고, 재연결 직후에도 현재 상태를 바로 알 수 있도록 첫 이벤트를 즉시 보낸다.
 * - publish(): 해당 주문을 구독 중인 모든 emitter에 상태 변경 이벤트를 브로드캐스트한다.
 * - heartbeat(): 모든 emitter에 주기적으로 PING을 보내 프록시/로드밸런서의 유휴 커넥션 종료를 막고,
 *   이미 끊어진 연결은 전송 실패 시점에 정리한다. 이름 있는 이벤트(`event: ping`)로 보내기 때문에
 *   브라우저 EventSource의 기본 onmessage(무명 message 이벤트만 수신)에는 잡히지 않는다.
 */
@Component
class OrderEventPublisher {

    companion object {
        private val log = LoggerFactory.getLogger(OrderEventPublisher::class.java)

        /** 이 시간이 지나면 서버가 먼저 연결을 끊는다 — 클라이언트의 재연결(Exponential Backoff) 로직이 다시 붙는다. */
        private const val EMITTER_TIMEOUT_MS = 10 * 60 * 1000L
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
    }

    private val emittersByOrderId = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    /** 새 SSE 구독을 등록하고, 현재 주문 상태를 첫 이벤트로 즉시 보낸다. */
    fun subscribe(orderId: Long, initialEvent: OrderDto.OrderTrackingEvent): SseEmitter {
        val emitter = SseEmitter(EMITTER_TIMEOUT_MS)
        emittersByOrderId.computeIfAbsent(orderId) { CopyOnWriteArrayList() }.add(emitter)

        emitter.onCompletion { remove(orderId, emitter) }
        emitter.onTimeout {
            remove(orderId, emitter)
            emitter.complete()
        }
        emitter.onError { remove(orderId, emitter) }

        sendMessage(orderId, emitter, initialEvent)

        return emitter
    }

    /** 주문 상태가 바뀔 때 호출한다. 해당 주문을 구독 중인 모든 클라이언트에 브로드캐스트한다. */
    fun publish(orderId: Long, event: OrderDto.OrderTrackingEvent) {
        val emitters = emittersByOrderId[orderId] ?: return
        emitters.toList().forEach { emitter -> sendMessage(orderId, emitter, event) }
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    fun heartbeat() {
        emittersByOrderId.forEach { (orderId, emitters) ->
            emitters.toList().forEach { emitter -> sendPing(orderId, emitter) }
        }
    }

    private fun sendMessage(orderId: Long, emitter: SseEmitter, event: OrderDto.OrderTrackingEvent) {
        try {
            emitter.send(SseEmitter.event().name("message").data(event, MediaType.APPLICATION_JSON))
        } catch (ex: Exception) {
            log.debug("주문 {} SSE 전송 실패, 구독 해제: {}", orderId, ex.message)
            emitter.completeWithError(ex)
            remove(orderId, emitter)
        }
    }

    private fun sendPing(orderId: Long, emitter: SseEmitter) {
        try {
            emitter.send(SseEmitter.event().name("ping").data("ping"))
        } catch (ex: Exception) {
            log.debug("주문 {} SSE 하트비트 전송 실패, 구독 해제: {}", orderId, ex.message)
            emitter.completeWithError(ex)
            remove(orderId, emitter)
        }
    }

    private fun remove(orderId: Long, emitter: SseEmitter) {
        emittersByOrderId[orderId]?.remove(emitter)
        if (emittersByOrderId[orderId]?.isEmpty() == true) {
            emittersByOrderId.remove(orderId)
        }
    }
}
