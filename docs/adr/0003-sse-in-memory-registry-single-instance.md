# 주문 추적은 인메모리 SSE 레지스트리로, 커밋 후에만 보낸다 (단일 인스턴스 전제)

주문 추적은 WebSocket이나 메시지 브로커 없이 `SseEmitter`로 한다. 구독자는 `OrderEventPublisher`의 인스턴스 메모리(`ConcurrentHashMap<orderId, emitters>`)에 있고, 구독 즉시 현재 상태를 보내며 15초마다 `ping` 이벤트로 연결을 유지한다. 주문 상태 변경은 `OrderStatusChangedEvent`를 발행하고 `@TransactionalEventListener(AFTER_COMMIT)`에서만 전송해, 롤백된 상태를 고객이 보는 일이 없게 했다. 클라이언트는 끊기면 지수 백오프로 재연결하다 3회 실패하면 REST 폴링으로 넘어간다.

근거: e12b374(최초 SSE), 22b8424 · PR #20(커밋 후 전송, 레지스트리 레이스 수정), README "동시성 · 정합성 처리".

## Consequences

- 알려진 한계: 구독자가 인스턴스 메모리에 있어 **단일 인스턴스 전제**다. 인스턴스를 늘리면 상태를 바꾼 인스턴스에 붙은 구독자만 이벤트를 받는다. 다중 인스턴스로 가려면 Redis Pub/Sub 같은 브로드캐스트 경로가 필요하다.
- 레지스트리 등록·해제는 `compute`/`computeIfPresent`로 키 단위 원자 연산 안에서 한다. "꺼내서 수정 후 비었으면 삭제"로 나누면 새 구독이 지워진다.
