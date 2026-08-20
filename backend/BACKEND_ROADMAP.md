# 스마트오더 백엔드 진행 상황 & 다음 할 일

> 작성일: 2026-08-19
> 기준 브랜치: `feature/gy/order`
> 이 문서는 프론트엔드 계약(타입/모의 API, `frontend/src/types`, `frontend/src/api`)과 실제 백엔드 코드(`backend/src/main/kotlin/com/gy/smartorder`)를 대조해 정리한 진행 상황 기록입니다. 진행하면서 체크박스를 갱신해주세요.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Store | ✅ 구현됨 | CRUD, Haversine 거리 계산, 커서 페이징 |
| Category | ✅ 구현됨 | CRUD |
| Menu | ✅ 구현됨 (일부 버그 수정함) | 옵션 그룹(온도/샷 추가 등) 미모델링 |
| Order | 🔶 진행 중 (2단계까지 완료) | 상태값/생성 플로우/멱등성 정합 완료 |
| Payment | ❌ 미구현 | 컨트롤러/서비스/엔티티 전부 없음 |
| Auth | ❌ 미구현 | `SecurityConfig`가 임시로 전체 permitAll |
| Cart | ➖ 백엔드 불필요 추정 | 프론트가 Zustand로 클라이언트에만 보관 (재확인 필요) |
| Coupon | ❌ 미구현 | |
| Member(적립/스탬프) | ❌ 미구현 | |
| Notification | ❌ 미구현 | |
| Event | ❌ 미구현 | PRD상 용도 불명확, 우선순위 낮음 |

## 2. 지금까지 한 일 (완료)

### 2.1 Order 계약 정합 (2단계) — 완료
- `OrderStatus` enum을 프론트 `order.types.ts`와 일치시킴: `PENDING, ACCEPTED, PREPARING, READY, PICKED_UP, CANCELLED` (기존엔 `PREPARING` 없이 `COMPLETED`를 썼음).
- 주문 생성 플로우를 검증 → 생성 2단계로 분리
  - `POST /orders/validate` 신규: 품절 메뉴를 `issues[]`로 반환 (`OrderValidateRequest/Response`, `OrderIssue`).
  - `POST /orders`: `packagingType`, `couponId`, `useStamp`, `idempotencyKey`, 옵션(`optionChoiceIds`)을 받고 가벼운 `{ orderId, totalAmount }`만 반환 (`OrderCreateResponse`).
- 멱등성 처리: `X-Idempotency-Key` 헤더 우선, 없으면 바디의 `idempotencyKey`로 폴백. `Order` 엔티티에 유니크 `idempotency_key` 컬럼 추가, 동시 요청 경쟁 상태는 `DataIntegrityViolationException` 캐치 후 기존 주문으로 재수렴.
- `ApiException`에 `details` 필드를 추가하고 `GlobalExceptionHandler`가 이를 실어 보내도록 수정 (전엔 항상 null로 새어나갔음) → 409 `ORDER_VALIDATION_FAILED` 시 `details: { issues: [...] }` 형태로 응답.
- `GET /orders/store/{storeId}` 응답을 프론트 `orderApi.storeQueue()`가 기대하는 `OrderHistoryItem` 모양(`OrderSummaryResponse`)으로 교체. 모든 id 필드를 프론트 `ID = string` 규칙에 맞춰 문자열로 직렬화.
- (부수 수정) `MenuDto.kt`에 `NotBlank`/`Min`/`MenuStatus`/`LocalDateTime` import가 통째로 빠져 있어 프로젝트 전체가 컴파일되지 않던 문제를 발견하고 수정함.

### 2.2 아직 검증 못한 것
- 이 작업 환경(Cowork 로컬 워크스페이스)에 네트워크가 없어 `./gradlew build`를 직접 돌려보지 못함. `build.gradle.kts`가 Spring Boot 4.1.0 / Kotlin 2.3.21 / Jackson 3(`tools.jackson`) 등 최신 버전을 쓰고 있어 IDE(IntelliJ)에서 **직접 빌드 확인 필요**.
- local 프로필은 `ddl-auto: create-drop`이라 다음 로컬 실행 시 새 컬럼/테이블(`idempotency_key`, `packaging_type`, `coupon_id`, `use_stamp`, `order_item_option_choice`)이 자동 생성됨. prod 프로필은 `ddl-auto: validate`라 실제 배포 전엔 별도 마이그레이션 스크립트(Flyway/Liquibase)가 필요함 — **아직 마이그레이션 도구 자체가 프로젝트에 없음**.

## 3. 다음에 할 일 (우선순위 순)

### [ ] 0. 브랜치 정리
- `feature/gy/order` 브랜치의 변경분이 일부 스테이징/일부 미스테이징으로 섞여 있고, `backend/README.md` 삭제 + 루트 `README.md` 신규 추가가 커밋 안 된 상태. 정리 후 커밋/PR로 마무리.
- 이번에 수정한 9개 파일(`common/exception/ApiException.kt`, `common/GlobalExceptionHandler.kt`, `controllers/order/OrderController.kt`, `dtos/menu/MenuDto.kt`, `dtos/order/OrderDto.kt`, `entities/order/Order.kt`, `entities/order/OrderItem.kt`, `repositories/order/OrderRepository.kt`, `services/order/OrderService.kt`)도 함께 커밋 필요.

### [ ] 1. 메뉴 옵션 그룹 도메인 (Order 2단계에서 발견된 선행 과제)
- 프론트 `menu.types.ts`의 `MenuOptionGroup`/`MenuOptionChoice`(온도, 샷 추가 등, `priceDelta` 포함)에 대응하는 백엔드 엔티티/테이블이 없음.
- 지금 `OrderItem.optionChoiceIds`는 값만 저장하고 가격에 반영하지 않는 임시 상태. 이 도메인이 생기면:
  - 옵션별 `priceDelta`를 주문 생성 시 가격 계산에 반영
  - `POST /orders/validate`에서 `OPTION_SOLD_OUT` 이슈 판정 가능해짐

### [ ] 2. 결제(Payment) 도메인 최소 구현
- `POST /payments/confirm` — `{ orderId, paymentKey, amount }` → `{ orderId, approvedAt }` (`paymentApi.ts`의 `ConfirmPaymentRequest/Response`와 대응).
- 결제 대기 → 결제 확인 상태 전이 필요 (현재 Order는 생성 즉시 `PENDING`으로 시작하는데, 결제 확인 전/후 상태 구분이 필요한지 PRD 재확인).
- PG 실연동은 이후 단계, 우선은 골격만.

### [ ] 3. 주문 추적 SSE + 폴링 폴백
- `GET /orders/{orderId}/events` (SSE) — `OrderTrackingEvent({ orderId, status, updatedAt, message })` 스트리밍.
- PING heartbeat 주기적 전송 (프로젝트 지침 4절 SSE 규약).
- 재연결 Exponential Backoff(1s,2s,4s,8s...) → 3회 실패 시 프론트가 3초 주기 REST 폴링으로 전환 — 이때 쓸 단건 조회는 기존 `GET /orders/{orderId}`를 응답 포맷만 `OrderTrackingEvent`에 맞추면 재활용 가능.

### [ ] 4. 인증(Auth) 도메인
- `SecurityConfig.kt`에 이미 남아있는 TODO: JWT 필터 추가, 매장 관리 API(POST/PATCH)는 `STORE_ADMIN` 권한으로 제한.
- `authApi.ts` 기준 필요 기능: 소셜 로그인, SMS 인증, 매장 관리자 로그인, 리프레시 토큰(Silent Refresh), 회원 탈퇴.
- 지금은 전체 `permitAll` 상태라 매장/메뉴 데이터를 누구나 수정 가능 — 배포 전 반드시 처리.

### [ ] 5. 나머지 도메인 필요 여부 재확인 후 착수
- **Cart**: 프론트에 `cartApi.ts`가 없고 Zustand 스토어로만 관리됨 → 백엔드 API 자체가 불필요할 가능성 있음. PRD 재확인 후 결정.
- **Coupon**: `authApi.ts`의 `coupons()`와 연동, 주문 시 `couponId` 할인 계산에 필요.
- **Member(적립/스탬프)**: `authApi.ts`의 `rewards()`와 연동, 주문 시 `useStamp` 할인 계산에 필요.
- **Notification**: 결제/상태 변경 시 푸시 알림 발송 (프로젝트 지침 5절 Vibration API와는 별개로, 서버발 푸시 트리거가 필요한지 확인).
- **Event**: PRD상 정확한 용도 확인 필요.

### [ ] 6. 인프라/운영
- 마이그레이션 도구(Flyway/Liquibase) 도입 — 지금 prod 프로필(`ddl-auto: validate`)은 스키마가 수동으로 맞춰져 있다고 가정하는데 실제 마이그레이션 스크립트가 없음.
- CI에서 `./gradlew build` 자동 검증 (이번처럼 import 누락이 로컬에서만 걸러지지 않도록).

## 4. 리스크 / 확인 필요 항목
- **ID 타입 불일치 가능성**: 프론트는 모든 id를 `ID = string` 타입으로 정의하지만, 백엔드는 `storeId`/`menuId` 등 요청 바디 필드를 여전히 `Long`으로 받음. Jackson이 JSON 문자열("123")을 `Long`으로 자동 변환하지 않을 수 있어, 실제 연동 시 400 에러가 날 수 있음. 프론트가 숫자 리터럴로 보내는지, 혹은 백엔드에서 문자열 허용 설정이 필요한지 확인 필요.
- **build.gradle.kts의 버전들**(Spring Boot 4.1.0, Kotlin 2.3.21, Jackson 3 `tools.jackson`)이 실제 존재/공개된 버전인지 재확인 필요 — 존재하지 않는 버전이면 빌드 자체가 안 됨.
