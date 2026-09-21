# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

카페 실시간 선주문 & 픽업 플랫폼 ("smart-order"): customers pre-order drinks from a store and track
preparation/pickup status in real time. This is a monorepo with two independently-run apps that share
no build tooling but must stay in contract sync:

- `backend/` — Kotlin + Spring Boot 4.1.0 REST API (Gradle, single module, `rootProject.name = "smart-order"`).
  `spring-boot-starter-data-redis` and `spring-boot-starter-kafka` are on the classpath but not yet wired
  into any domain logic — treat them as available infra, not as something currently in active use.
- `frontend/` — Next.js (App Router) + TypeScript client (`npm`).

The backend is the newer, actively-developed side; the frontend's mock layer (`frontend/src/api/mock`,
`frontend/src/types`) is the source of truth for request/response shapes the backend must match — see
"Frontend contract syncing" below.

`BACKEND_ROADMAP.md` (repo root) tracks current backend implementation status per domain and the
prioritized next-steps list; check it before assuming a domain (Auth, Coupon, Member, Cart, Notification)
is implemented.

## Commands

All backend commands run from `backend/`; all frontend commands run from `frontend/`.

### Backend (Kotlin / Spring Boot / Gradle)
```bash
./gradlew build              # compile + run all checks + tests
./gradlew bootRun            # run the API locally (defaults to the `local` profile: H2 in-memory DB)
./gradlew test               # run all tests
./gradlew test --tests "com.gy.smartorder.common.GeoUtilsTest"              # single test class
./gradlew test --tests "com.gy.smartorder.common.GeoUtilsTest.methodName"   # single test method
```
There is no linter/formatter configured (no ktlint/detekt plugin in `build.gradle.kts`) — `./gradlew build`
is the only gate.

Local profile (`application-local.yaml`) uses an in-memory H2 DB with `ddl-auto: create-drop` (schema is
regenerated from entities on every start, H2 console at `/h2-console`). The `prod` profile
(`application-prod.yaml`) uses PostgreSQL with `ddl-auto: validate` — there is **no migration tool
(Flyway/Liquibase) yet**, so entity changes have no accompanying prod schema script (tracked as a known gap
in `BACKEND_ROADMAP.md`).

### Frontend (Next.js)
```bash
npm run dev      # dev server
npm run build    # production build
npm run lint     # eslint
```
No test runner is configured in `package.json`.

## Backend architecture

### Package-by-feature
Code is organized by domain/feature (`store`, `category`, `menu`, `order`, `payment`, `auth`, `member`,
`coupon`, ...), and each domain package holds every layer for that feature flat in one folder — there are
no top-level `controllers/`, `dtos/`, `entities/`, `repositories/`, `services/` packages:
```
<domain>/XxxController.kt   → thin: delegates straight to the service, maps status codes
<domain>/XxxDto.kt          → a single `class XxxDto` wrapping request/response data classes as nested
                                types (e.g. `OrderDto.OrderCreateRequest`), each with a
                                `companion object fun from(entity): Response` mapper
<domain>/Xxx.kt             → JPA entity, `@EntityListeners(AuditingEntityListener::class)` +
                                `@CreatedDate`/`@LastModifiedDate` for timestamps
<domain>/XxxRepository.kt   → plain `JpaRepository<Entity, Long>` with derived-query methods
<domain>/XxxService.kt      → `@Service @Transactional(readOnly = true)` at class level, with
                                `@Transactional` overridden per mutating method
```
A domain package may have its own sub-package for internal-only grouping (e.g. `auth/oauth/` holds the
Kakao/Apple token verifiers used only by `auth/AuthService.kt`) — that's still one feature, just organized
internally. `coupon/` has no controller (it's exposed through `member/MemberController.kt`); `auth/` has no
entity of its own (it operates on `member/Member.kt`). Test sources under `src/test/kotlin` mirror this:
`<domain>/XxxServiceTest.kt` in the same `com.gy.smartorder.<domain>` package, not under a layer folder.

`common/` holds cross-domain utilities (`GeoUtils` — Haversine distance, `CursorUtils` — offset-based
cursor pagination) and the exception/error-response pair described below; `config/` holds framework-wide
setup (security, JWT). Neither is a feature domain. New domains should follow this same flat-per-feature
shape rather than introducing a different pattern (e.g. a layer-based split).

### Error handling
`ApiException(status, code, message, details: Map<String, List<String>>? = null)` is the base for all
domain errors; `NotFoundException`, `BadRequestException`, `ConflictException` are its subtypes (mapped to
404/400/409). `GlobalExceptionHandler` catches `ApiException` and `MethodArgumentNotValidException` and
renders `ErrorResponse(code, message, details?)`, which is a fixed contract with the frontend's
`ApiErrorBody`. When adding a new failure mode, prefer throwing one of the existing `ApiException`
subtypes (or adding a new one) over ad hoc exceptions, so it round-trips through this handler.

### Order domain: idempotency, snapshotting, SSE tracking
- **Two-step create**: `POST /orders/validate` (re-check sale status against current server state) then
  `POST /orders` (actually create). `OrderService.validateItems()` backs both.
- **Idempotency**: `POST /orders` requires `X-Idempotency-Key` header (falls back to the request body's
  `idempotencyKey` field if the header is absent). The key has a DB unique constraint
  (`Order.idempotencyKey`); `OrderService.createOrder()` first looks up an existing order by that key, and
  also catches `DataIntegrityViolationException` on save to handle the concurrent-duplicate-request race by
  re-fetching and returning the existing order instead of erroring. `PaymentService.confirmPayment()`
  applies the identical pattern (lookup-by-key, then catch-and-refetch-on-save) keyed on `paymentKey` and
  on `orderId` (a `Payment` is unique per `Order`).
- **Snapshotting**: `OrderItem` copies `menuName`/`price` from `Menu` at order time so historical orders
  stay accurate if the menu changes later. `optionChoiceIds` is stored but not priced — there is no menu
  option/choice domain yet, so `priceDelta` is not applied to `totalPrice` (see roadmap item "메뉴 옵션 그룹
  도메인").
- **Real-time tracking**: `GET /orders/{orderId}/events` is an SSE stream backed by `OrderEventPublisher`,
  an in-memory `ConcurrentHashMap<orderId, CopyOnWriteArrayList<SseEmitter>>` registry (per-instance state —
  not safe across multiple backend instances; the roadmap notes migrating this to Redis Pub/Sub, since
  Redisson is already a dependency, before scaling out). It sends the current state immediately on
  subscribe, broadcasts on `OrderService.updateOrderStatus()`, and sends a named `ping` event every 15s
  (`@Scheduled`, requires `@EnableScheduling` on the application class) to keep proxies from closing idle
  connections — intentionally a *named* event so the browser `EventSource` default `onmessage` handler
  doesn't see it. Clients are expected to reconnect with exponential backoff and fall back to polling
  `GET /orders/{orderId}` after repeated failures.

### Frontend contract syncing
Backend DTOs, enum values, and error shapes are deliberately kept 1:1 with frontend TypeScript types
(`frontend/src/types/*.types.ts`) and the frontend's mock API implementations
(`frontend/src/api/mock`, `frontend/src/api/*Api.ts`). DTO/entity doc comments frequently point at the
specific frontend file/type they must match (e.g. `OrderStatus` ↔ `order.types.ts`, `OrderTrackingEvent` ↔
`OrderTrackingEvent`/`orderTrackingMock.ts` message copy, `CursorUtils` ↔ `storeMock.ts`'s
`btoa(JSON.stringify({ offset }))` format). When changing a DTO shape or enum, check the corresponding
frontend type/mock file rather than assuming the backend is free to diverge. Note a known open risk: the
frontend types most IDs as `string`, while the backend still binds `storeId`/`menuId`/`orderId` etc. as
`Long` in request bodies.

### Auth
`SecurityConfig` runs a stateless JWT filter chain (`JwtAuthenticationFilter` ahead of
`UsernamePasswordAuthenticationFilter`, no sessions). Only a short allowlist is `permitAll`
(`/auth/**`, Swagger/docs, `/h2-console/**`, `/actuator/**`) plus `GET /stores/**` (customers can browse
stores before logging in); `/admin/**` requires `ADMIN`, `PATCH /stores/**` requires `STORE_ADMIN`
(issued by `POST /auth/store-login`), and everything else falls through to `anyRequest, authenticated`
(logged-in member, no role check) — that includes the menu/menu-option admin write endpoints, which don't
yet have their own `STORE_ADMIN` restriction (see `backend/PROGRESS.md`). Social login (Kakao/Apple, with
server-side token verification), SMS auth, store-admin login, and refresh-token issuance/invalidation are
all implemented under `auth/`.
