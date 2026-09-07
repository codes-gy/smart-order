# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-07 (1.3절 — Notification 도메인 "백엔드 불필요" 확인 완료)
> 기준 브랜치: `feature/gy/notification` (base: `develop`, merge-base에 PR #5 Auth, PR #6 Coupon, PR #7
> Member 적립/스탬프가 이미 병합돼 있음)
> 이 문서는 로컬 Claude Code CLI 세션이 관리합니다. `BACKEND_ROADMAP.md`는 Cowork 세션이 별도로 관리하는
> 문서이니 혼동하지 말 것. 이전 브랜치(`feature/gy/auth`, `feature/gy/coupon`, `feature/gy/member-stamp`)의
> 작업 기록은 각각 PR #5, #6, #7로 병합 완료돼 이 문서에서는 정리했다 — 상세 이력은 `git log`/PR 참고.
>
> **⚠️ 확인된 중복 작업**: `origin/feature/gy/member-reward`에 다른 세션(추정: Cowork)이 이미 병합된 PR #7과
> 거의 동일한 내용(Member 적립/스탬프)을 독립적으로 구현해 push해둔 상태. PR은 아직 안 열렸음. develop에
> 이미 PR #7이 병합됐으니 그 브랜치는 더 이상 필요 없음 — 확인 후 삭제 권장(`git push origin
> --delete feature/gy/member-reward`), 단 다른 세션이 아직 쓰고 있을 수 있으니 삭제 전 확인 필요.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Auth / Store / Category / Menu / Order / Payment | ✅ `develop` 기준 구현됨 | |
| Coupon | ✅ `develop` 기준 구현됨 (PR #6) | 회원가입 시 웰컴 쿠폰 자동 발급 + 주문 시 소비 |
| Member 적립/스탬프 | ✅ `develop` 기준 구현됨 (PR #7) | 주문 픽업완료 시 1개 적립, 10개 모으면 4500원 정액 할인으로 사용 |
| Notification | ➖ 백엔드 작업 불필요로 확인 (이 브랜치) | 프론트가 기존 SSE + 브라우저 Notification API만으로 이미 완결 구현 |

## 2. 지금까지 한 일 (이 브랜치)

### 2.1 Notification 도메인 — "백엔드 작업 불필요" 확인, 결론만 문서화
- **작업 안 함**: 코드 변경 없음. `BACKEND_ROADMAP.md`가 남겨둔 "결제/주문 상태 변경 시 서버발 푸시 트리거
  필요 여부부터 재확인"에 대한 답을 프론트 코드를 직접 읽어 확정했다.
- **근거**:
  - `frontend/src/hooks/usePushNotificationPermission.ts` 주석: "실제 푸시 서버(Web Push) 없이도 픽업
    준비 완료 시 포그라운드 알림을 데모할 수 있도록 한다" — 브라우저 `Notification` 권한 요청만 감싼 훅.
  - `frontend/src/routers/OrderTrackingRouter.tsx:37`: 기존 SSE(`GET /orders/{orderId}/events`)로 받는
    `OrderTrackingEvent.message`를 그대로 `new Notification("스마트오더", { body: event.message })`에
    넘겨 로컬 브라우저 알림을 띄움. 서버가 푸시를 보내는 게 아니라 **클라이언트가 이미 연결돼 있는 SSE를
    보고 스스로 알림을 생성**하는 구조.
  - FCM/APNs/Web Push 구독 토큰 등록·저장, 서버발 트리거 엔드포인트를 요구하는 프론트 API 계약이나 mock이
    전혀 없음 (`frontend/src/api/mock/` 전체 검색 결과 없음).
- **사용자 확인**: "백엔드 불필요 확인으로 종료" 방향으로 결정(대안이었던 "탭이 닫혀있어도 알림 오는 실제
  Web Push 구현"은 프론트 계약에 없는 새 기능이라 범위 밖으로 보류).
- `PROGRESS.md`/`BACKEND_ROADMAP.md`의 도메인 상태만 갱신하고 다음 우선순위(Event)로 넘어간다.

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료 (PR #6, `develop` 병합됨)
### [x] 1. Member 적립/스탬프 도메인 — 완료 (PR #7, `develop` 병합됨)
### [x] 2. Notification 도메인 — 백엔드 작업 불필요로 확인, 2026-09-07 (이 브랜치)
- 2.1절 참고. 프론트가 SSE + 브라우저 Notification API로 이미 완결.

### [ ] 3. Event 도메인
- PRD상 정확한 용도 확인 필요. 우선순위 가장 낮음.

### [ ] 4. Cart 도메인 필요 여부 재확인
- 프론트가 Zustand로 클라이언트에만 보관 중 — 백엔드 API 자체가 불필요할 가능성.

### [ ] 5. 메뉴 옵션 관리자 CRUD
- 매장 관리자가 옵션 그룹/선택지를 등록·수정·삭제·품절처리 하는 기능(조회/주문 반영은 이미 완료).

### [ ] 6. 인프라/운영
- 마이그레이션 도구(Flyway/Liquibase) 도입 — prod 프로필(`ddl-auto: validate`)용 스키마 스크립트 없음.
  이번에 `coupons` 테이블도 추가돼 관리 대상 테이블이 더 늘어남.
- CI에서 `./gradlew build` 자동 검증.

## 4. 리스크 / 확인 필요 항목
- **쿠폰 발급 경로가 웰컴 쿠폰뿐**: 관리자가 프로모션 쿠폰을 임의로 발급하는 기능은 아직 없음. 필요해지면
  별도 우선순위로(관리자 발급 API 신설 또는 `POST /stores/{storeId}/coupons` 등).
- **(해결됨, PR #6)** ~~Order 동시 요청 경쟁 상태로 쿠폰 이중 소비 방지가 불완전~~ — `ConflictException` 캐치 후
  재조회하는 방식으로 수정 완료.
- **DB 마이그레이션 도구 부재**: `coupons`, `member.stamp_count` 컬럼 포함, prod 스키마 스크립트가 여전히
  없음(3절 6번 참고).
- **스탬프 적립 시점은 PRD에 명시되지 않아 판단으로 결정함**: "주문이 `PICKED_UP`(픽업 완료) 상태로 전환될
  때 1개 적립"으로 구현(생성 시점이 아니라 완료 시점 — 취소된 주문에는 적립 안 됨). PRD/기획 의도와 다르면
  `OrderService.updateOrderStatus()`의 `justPickedUp` 조건만 바꾸면 됨.
