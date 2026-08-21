# 스마트오더 백엔드 진행 상황 & 다음 할 일

> 최종 갱신: 2026-08-21
> 기준 브랜치: `feature/gy/payment` (Order → Payment → SSE 추적 → 메뉴 옵션 순으로 이 브랜치에서 계속 진행됨)
> 이 문서는 프론트엔드 계약(타입/모의 API, `frontend/src/types`, `frontend/src/api`)과 실제 백엔드 코드(`backend/src/main/kotlin/com/gy/smartorder`)를 대조해 정리한 진행 상황 기록입니다. 진행하면서 체크박스를 갱신해주세요.
>
> **⚠️ 작업 충돌 주의**: 로컬에서 Claude Code CLI(`CLAUDE.md`, `backend/.claude/`)를 별도로 쓰고 계신 걸 확인했어요. 이 문서는 Cowork 세션이 관리합니다 — 로컬 세션과 같은 파일을 동시에 건드리면 덮어쓰기가 날 수 있으니, 한쪽 작업을 끝내고 커밋한 뒤 다른 쪽을 진행해주세요.
> **이 파일이 두 번이나 커밋 안 된 채로 유실된 적이 있어요. 매번 작업 후 꼭 `git add BACKEND_ROADMAP.md`로 커밋해주세요.**

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Store | ✅ 구현됨 | CRUD, Haversine 거리 계산, 커서 페이징 |
| Category | ✅ 구현됨 | CRUD |
| Menu | ✅ 구현됨 (옵션 그룹 포함) | 옵션 그룹/선택지 조회 + 주문 반영까지 완료. 관리자용 옵션 CRUD는 아직 없음 |
| Order | 🔶 진행 중 (계약 정합 + SSE + 옵션 가격 반영 완료) | 생성/조회/상태변경/실시간추적/옵션가격 모두 완료 |
| Payment | 🔶 최소 구현 완료 | 결제 승인 기록/멱등 처리만, PG 실연동 전 |
| Auth | ❌ 미구현 | `SecurityConfig`가 임시로 전체 permitAll |
| Cart | ➖ 백엔드 불필요 추정 | 프론트가 Zustand로 클라이언트에만 보관 (재확인 필요) |
| Coupon | ❌ 미구현 | |
| Member(적립/스탬프) | ❌ 미구현 | |
| Notification | ❌ 미구현 | |
| Event | ❌ 미구현 | PRD상 용도 불명확, 우선순위 낮음 |

## 2. 지금까지 한 일 (완료)

### 2.1 Order 계약 정합 — 완료 (커밋됨)
- `OrderStatus` enum을 프론트 `order.types.ts`와 일치: `PENDING, ACCEPTED, PREPARING, READY, PICKED_UP, CANCELLED`.
- 주문 생성 플로우를 검증(`POST /orders/validate`) → 생성(`POST /orders`) 2단계로 분리.
- `X-Idempotency-Key` 헤더(폴백: 바디) 기반 멱등 처리, DB 유니크 제약 + 경쟁 상태 방어.
- `ApiException`에 `details` 필드 추가, 409 `ORDER_VALIDATION_FAILED` 시 `details: { issues: [...] }` 형태로 응답.
- `GET /orders/store/{storeId}`를 프론트 `OrderHistoryItem` 모양(`OrderSummaryResponse`)으로 교체.
- (부수 수정) `MenuDto.kt`의 누락된 import 수정.

### 2.2 Payment 최소 구현 — 완료 (커밋됨)
- `POST /payments/confirm` 신규 (`payment` 도메인 전체 신설).
- 결제 금액 검증(`amount` != `order.totalPrice` → 409), 결제 키/주문 기준 멱등 처리 2중.
- 결제 승인이 `Order.status`를 자동으로 바꾸지는 않음 (설계상 결정, PRD 재확인 필요 항목으로 남겨둠).
- PG 서버 측 재검증은 아직 없음(TODO로 명시).

### 2.3 주문 추적 SSE + 폴링 폴백 — 완료 (커밋됨)
- `GET /orders/{orderId}/events` (SSE). 신규 `services/order/OrderEventPublisher.kt`가 주문별 `SseEmitter` 구독자 레지스트리 + 브로드캐스터 역할.
- 구독 즉시 현재 상태를 첫 이벤트로 전송, 15초 주기 `event: ping` 하트비트(이름 있는 이벤트라 프론트 기본 `onmessage`에 안 걸림), 10분 emitter 타임아웃(만료 시 클라이언트 재연결 로직이 자연스럽게 재작동).
- `PATCH /orders/{orderId}/status` 변경 시 `orderRepository.flush()` 후 브로드캐스트.
- `SmartOrderApplication.kt`에 `@EnableScheduling` 추가.
- REST 폴링 폴백은 기존 `GET /orders/{orderId}` 재사용.

### 2.4 메뉴 옵션 그룹 도메인 — 완료 (조회 + 주문 반영 범위, 아직 미커밋)
- 신규 엔티티 `entities/menu/MenuOptionGroup.kt`, `entities/menu/MenuOptionChoice.kt` (Menu 1:N 그룹, 그룹 1:N 선택지).
  - `MenuOptionType`(SINGLE/MULTIPLE)은 JPA 내부용. 프론트 `OptionType`이 소문자("single"/"multiple")라 Jackson enum 기본 직렬화와 안 맞아서, 응답 DTO(`MenuOptionGroupResponse`)에서 수동으로 소문자 문자열 변환.
- `Menu.kt`에 `optionGroups: MutableList<MenuOptionGroup>` 양방향 연관관계 추가.
- `MenuDto.MenuResponse`에 `optionGroups` 필드 추가 — `GET /menus/{id}`, `GET /menus?categoryId=` 응답에 자동으로 실림.
- `OrderService`:
  - `validateItems`에 `OPTION_SOLD_OUT` 판정 추가 (`{메뉴명}의 '{옵션명}' 옵션이 품절되었어요.` — 프론트 mock 문구와 동일).
  - `createOrder`에서 `optionChoiceIds`의 `priceDelta` 합을 `menu.price`에 더해 `OrderItem.price`(옵션 반영 단가) 계산 → `totalPrice`에 정확히 반영.
- **범위 결정**: 매장 관리자가 옵션 그룹/선택지를 등록·수정·삭제하는 CRUD는 이번 범위에서 제외 (조회 + 주문 반영까지만). 필요해지면 Category/Menu CRUD와 같은 패턴으로 추가하면 됨.
- 존재하지 않는(메뉴에 속하지 않는) `optionChoiceId`가 들어오면 조용히 무시함 — 프론트 mock의 기존 동작과 동일하게 맞춘 것.
- 로컬 빌드 검증 아직 필요.

### 2.5 (참고) 작업 중 발견한 이슈
- `backend/build.gradle.kts`: 사용자님이 `firebase-database-ktx`(안드로이드 전용) → `firebase-admin`으로 교체 + `google()` 저장소 추가함. 빌드 성공 원인으로 보임.
- **로컬 Claude Code CLI와의 작업 충돌**: 옵션 도메인 작업 중 로컬에 이미 스테이징된 빈 스텁(`entities/menu/MenuOption.kt`, `controllers/menu/MenuOptionController.kt`)을 발견함 — 로컬 Claude Code CLI 세션이 같은 기능을 동시에 만들려던 흔적. 사용자님 확인 하에 이 Cowork 세션 구현(`MenuOptionGroup`/`MenuOptionChoice`)으로 통일하기로 하고, 스텁 파일은 `git rm`이 아니라 (git lock이 다른 프로세스에 걸려 있어서) `_to_delete/` 폴더로 옮겨뒀음. **사용자님이 직접 `_to_delete/` 폴더를 지우고, `git status`로 `MenuOption.kt`/`MenuOptionController.kt`가 완전히 정리됐는지 확인해주세요.**

## 3. 다음에 할 일 (우선순위 순)

### [ ] 0. 커밋 정리 + 로컬 세션 충돌 정리
- `_to_delete/` 폴더 삭제, 스테이징 정리(`git add -A` 또는 개별 `git restore --staged`).
- 메뉴 옵션 그룹 변경분(Menu.kt, MenuDto.kt, OrderItem.kt, OrderService.kt 수정 + MenuOptionGroup.kt/MenuOptionChoice.kt 신규) 커밋.
- 로컬 Claude Code CLI 세션과 이 Cowork 세션 중 어느 쪽이 다음 작업을 이어갈지 정하고, 안 쓰는 쪽은 잠시 멈춰주세요 (동시 편집 방지).
- 이 로드맵 문서도 커밋.

### [ ] 1. 인증(Auth) 도메인 (다음 최우선 추천)
- `SecurityConfig.kt`의 TODO: JWT 필터 추가, 매장 관리 API는 `STORE_ADMIN` 권한으로 제한.
- 필요 기능(`authApi.ts` 기준): 소셜 로그인, SMS 인증, 매장 관리자 로그인, 리프레시 토큰, 회원 탈퇴.
- 지금은 전체 `permitAll`이라 매장/메뉴/결제 확인/SSE 구독 엔드포인트까지 누구나 호출 가능 — 배포 전 반드시 처리.

### [ ] 2. 나머지 도메인 필요 여부 재확인 후 착수
- **Cart**: 백엔드 API 불필요 가능성 (Zustand 클라이언트 보관) — PRD 재확인 후 결정.
- **Coupon**: 주문 시 `couponId` 할인 계산에 필요.
- **Member(적립/스탬프)**: 주문 시 `useStamp` 할인 계산에 필요.
- **Notification**: 결제/상태 변경 시 서버발 푸시 트리거 필요 여부 확인.
- **Event**: PRD상 정확한 용도 확인 필요.

### [ ] 3. 메뉴 옵션 관리자 CRUD (필요해지면)
- 매장 관리자가 옵션 그룹/선택지를 등록·수정·삭제·품절처리 하는 기능. Category/Menu CRUD와 같은 패턴.

### [ ] 4. 인프라/운영
- 마이그레이션 도구(Flyway/Liquibase) 도입 — prod 프로필(`ddl-auto: validate`)용 스키마 스크립트가 아직 없음. 테이블이 계속 늘고 있어서(`payment`, `menu_option_group`, `menu_option_choice`, `order_item_option_choice`) 더 늦기 전에 도입 권장.
- CI에서 `./gradlew build` 자동 검증.

## 4. 리스크 / 확인 필요 항목
- **ID 타입 불일치 가능성**: 프론트는 모든 id를 `ID = string`으로 정의하지만, 백엔드는 요청 바디의 `storeId`/`menuId`/`orderId` 등을 여전히 `Long`으로 받음. Jackson이 JSON 문자열을 `Long`으로 자동 변환하지 않으면 실연동 시 400 에러 가능 — 프론트가 숫자 리터럴로 보내는지 확인 필요.
- **PG 실연동 전 결제 위변조 리스크**: 지금 `/payments/confirm`은 클라이언트가 보낸 금액/키를 서버가 주문 금액과만 대조할 뿐, PG사에 실제로 그 결제가 있었는지 재확인하지 않음.
- **결제 완료 시 주문 상태 자동 전환 여부**: 현재는 결제 승인과 `OrderStatus` 전환이 분리돼 있음. PRD 의도와 맞는지 확인 필요.
- **SSE 구독자 레지스트리가 인스턴스 메모리에 있음**: 다중 인스턴스로 스케일아웃하면 Redis Pub/Sub으로 브로드캐스트를 릴레이하는 구조로 바꿔야 함. 지금 단일 인스턴스 개발 단계라 문제 없음.
- **여러 작업 세션 동시 사용**: 이 Cowork 세션과 로컬 Claude Code CLI를 동시에 켜두면 같은 파일을 서로 다르게 고치다 충돌할 수 있음. 가능하면 한 번에 한쪽만 활성화해주세요.
