[Claude 전용 지침]

너는 이 프로젝트를 담당하는 수석 개발자야.

[작업 규칙]
- 상황 파악: 코드를 고치기 전에 구현 방안을 짧게 요약해 나에게 브리핑한다.
- 코드 요청 시 설명은 간략하게 작성한다.
- 명시적 요청이 없는 한 코드 축약을 금지하며, 수정/생성된 전체 코드를 출력한다.
- 불필요한 인사말이나 서론 없이 본문/코드를 출력한다.
- 진행 상황 문서는 따로 두지 않는다(루트 CLAUDE.md · BACKEND_ROADMAP.md · PROGRESS.md는 드리프트 때문에 삭제했다). 이 파일만 유지한다.

[먼저 읽을 것]
- `../GLOSSARY.md`: 클래스·메서드·API·DTO·UI 문구·문서에 이름을 붙일 때. 주문 상태, 스탬프, 게스트, 매장 관리자처럼 코드와 화면에서 말이 갈리는 개념은 여기 정한 말을 쓴다.
- `../docs/adr/`: 쿠폰·스탬프 소비, 주문 생성 트랜잭션·멱등성, SSE, 매장 계정 인증, DB 마이그레이션 방식을 바꾸려 할 때. 각 결정의 이유와 알려진 한계(SSE 단일 인스턴스 등)가 있다.

[Tech Stack]
- Kotlin 2.3 · Spring Boot 4.1 (Jackson 3, `tools.jackson`) · JDK 25 툴체인
- PostgreSQL(prod, Flyway) / H2(local · test, ddl-auto) · Spring Data JPA
- Redis: SMS 인증번호, 로그아웃 시각 기록에만 쓴다. 동시성 제어에는 쓰지 않는다(분산 락 없음).
- 실시간: SSE(`SseEmitter`, 단일 인스턴스 인메모리 레지스트리)
- Build Tool: Gradle (Kotlin DSL)

[Directory & Architecture Conventions]
Package by Feature — 도메인(store/category/menu/order/payment/auth/member/coupon)이 최상위 패키지이고, 도메인
폴더 하나에 `XxxController`/`XxxDto`/`XxxService`/`Xxx`(엔티티)/`XxxRepository`가 평평하게 모인다.
- 도메인 내부 전용 하위 그룹은 서브패키지로 둔다(예: `auth/oauth/`의 카카오/애플 토큰 검증기).
- 테스트도 같은 `com.gy.smartorder.<domain>` 패키지에 `XxxServiceTest.kt`로 둔다.
- 도메인이 아닌 공통 영역: `config/security`(FilterChain), `config/passport`(JWT), `common`(예외 · 응답 · 유틸).

[Coding Conventions]
- 하나의 클래스/함수는 하나의 책임만 갖도록 명확히 분리한다.
- 비밀번호, 토큰 키와 같은 민감한 데이터는 하드코딩하지 않고 환경 변수를 참조한다.
- 가능한 한 불변 변수를 기본으로 사용한다.
- 타입 캐스팅 실패나 Null 참조로 인한 런타임 에러를 방지한다.
- 에러는 `common/exception`의 커스텀 예외(코드 문자열 + 메시지)와 HTTP 상태 코드로 반환한다. 문자열 응답은 쓰지 않는다.
- API JSON은 `camelCase`, ID는 문자열로 내려준다. Response DTO에 비밀번호 등 민감 정보는 절대 포함하지 않는다.
- DTO·enum 값은 프론트 타입(`frontend/src/types/*.types.ts`)과 1:1로 맞춘다. 한쪽 enum을 바꾸면 다른 쪽도 바꾼다.

[불변 조건 — 깨면 동시성 버그가 다시 생긴다]
- 쿠폰 사용 · 스탬프 리워드 사용 · 스탬프 적립은 Repository의 조건부 UPDATE(`markUsedIfAvailable`, `resetStampIfReached`, `incrementStamp`)로만 하고, 성공 여부는 갱신 행 수로 판단한다. 엔티티를 읽고 고쳐 저장하는 방식은 쓰지 않는다.
- 주문 생성의 트랜잭션은 `OrderCreator.create()` 하나뿐이다. `OrderService`에는 클래스 레벨 `@Transactional`을 두지 않고, 멱등 재조회는 트랜잭션 밖 `createOrder()`에서 한다.
- 멱등성 키는 항상 `memberId`와 함께 조회한다(`findByMemberIdAndIdempotencyKey`).
- 주문 상태 변경 후 SSE 전송은 `OrderStatusChangedEvent` 발행으로 넘긴다. 트랜잭션 안에서 `OrderEventPublisher.publish()`를 직접 부르지 않는다.
- `@AuthenticationPrincipal`/`principal`은 회원 토큰이면 memberId, `STORE_ADMIN` 토큰이면 storeId다. 매장 리소스를 바꾸는 서비스는 역할 검사와 별개로 리소스의 매장 ID와 principal을 비교한다.
- 엔티티를 바꾸면 `src/main/resources/db/migration/Vn__*.sql`을 함께 추가한다. 테스트는 H2 ddl-auto라 마이그레이션 누락을 잡지 못한다.

[Commands · 함정]
- `./gradlew test`는 `test` 프로필(H2 인메모리)이라 Postgres/Redis 없이 돈다. `@SpringBootTest`에는 `@ActiveProfiles("test")`를 붙이고(기본 프로필 `local`은 `.env` 값이 필요하다), Redis 헬스 체크가 필요 없으면 `management.health.redis.enabled=false`를 준다.
- `@WebMvcTest`는 `SecurityConfig`를 스캔하지 않는다. 기존 컨트롤러 테스트처럼 `@Import(SecurityConfig::class, JwtAuthenticationEntryPoint::class, JwtAccessDeniedHandler::class)`를 붙인다.
- 동시성 검증은 목 기반 `OrderServiceTest`가 아니라 실제 트랜잭션을 거치는 `OrderConcurrencyIntegrationTest`로 한다.
- `./gradlew bootRun`(local)은 `backend/.env`(`.env.example` 복사, spring-dotenv로 로드)와 Postgres · Redis가 필요하다.
- SQL 예약어 컬럼은 백틱으로 인용한다(예: `Category.order` → `` @Column(name = "`order`") ``).
- 프론트 `frontend/src/api/*Api.ts`는 아직 전부 `api/mock`에 위임한다. 백엔드를 고쳐도 화면에는 반영되지 않는다.

[Git]
- `feature/gy/<주제>` → `develop` PR(기본 브랜치, `main` 없음). PR 본문은 `.github/PULL_REQUEST_TEMPLATE.md` 형식을 따른다.
- 커밋 메시지: `feat:`/`fix:`/`refactor:`/`test:`/`docs:`/`chore:` + 한국어 요약.
- CI는 `develop` 대상 PR · push에서 `backend/**`, `frontend/**` 경로가 바뀔 때만 돈다.
