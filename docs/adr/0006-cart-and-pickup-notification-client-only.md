# 장바구니와 픽업 알림은 백엔드 도메인을 두지 않고 프론트엔드에서 완결한다

로드맵에 Cart·Notification 도메인이 있었지만 프론트 코드를 확인한 결과 둘 다 서버 API가 필요 없다고 결론 내렸다. 장바구니는 Zustand `persist`로 브라우저 `localStorage`에만 있고, 체크아웃 때 `POST /orders/validate` → `POST /orders`로 바로 주문이 된다. 픽업 알림은 주문 추적 SSE를 받은 브라우저가 `Notification` API로 띄운다. Event(프로모션) 도메인은 근거가 되는 PRD가 없어 보류했다.

근거: PR #8(Notification), PR #9(Event 보류), PR #10(Cart).

## Consequences

- 기기를 바꾸거나 브라우저 저장소를 지우면 장바구니가 사라진다. 기기 간 동기화 요구는 현재 범위 밖이다.
- 앱이나 탭이 닫혀 있으면 픽업 알림이 오지 않는다. FCM·Web Push 같은 서버발 푸시는 프론트 계약에 없어 하지 않았다.
