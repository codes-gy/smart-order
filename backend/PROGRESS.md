# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-21 (2.3절 — Flyway 기반 DB 마이그레이션 도입, `feature/gy/db-migration` 브랜치)
> 기준 브랜치: `feature/gy/db-migration` (base: `develop`, merge-base에 PR #5~#12가 이미 병합돼 있음)
> 이 문서는 로컬 Claude Code CLI 세션이 관리합니다. `BACKEND_ROADMAP.md`는 Cowork 세션이 별도로 관리하는
> 문서이니 혼동하지 말 것. 이전 브랜치들의 작업 기록은 각각 PR #5~#8로 병합 완료돼 이 문서에서는 정리했다
> — 상세 이력은 `git log`/PR 참고. PR 본문은 앞으로 `.github/PULL_REQUEST_TEMPLATE.md` 형식(작업
> 유형/작업 내용/고민한 내용/체크리스트/테스트/참고 사항)을 따른다.
>
> **⚠️ 확인된 중복 작업 (미해결)**: `origin/feature/gy/member-reward`에 다른 세션(추정: Cowork)이 이미
> 병합된 PR #7과 거의 동일한 내용(Member 적립/스탬프)을 독립적으로 구현해 push해둔 상태. PR은 아직 안
> 열렸음. develop에 이미 PR #7이 병합됐으니 그 브랜치는 더 이상 필요 없음 — 확인 후 삭제 권장(`git push
> origin --delete feature/gy/member-reward`), 단 다른 세션이 아직 쓰고 있을 수 있으니 삭제 전 확인 필요.
>
> **⚠️ 오래된 중복 문서 발견 (미해결)**: `backend/BACKEND_ROADMAP.md`가 저장소 루트 `BACKEND_ROADMAP.md`와
> 별개로 git에 추적돼 있음. Auth 도메인이 아직 미구현이던 시절 스냅샷이라 지금은 완전히 stale — 루트
> 문서가 진짜 최신본. 정리(삭제) 필요하지만 이 문서는 Cowork 세션이 관리하는 영역이라 확인 없이 건드리지
> 않았음.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Auth / Store / Category / Menu / Order / Payment | ✅ `develop` 기준 구현됨 | |
| Coupon | ✅ `develop` 기준 구현됨 (PR #6) | 회원가입 시 웰컴 쿠폰 자동 발급 + 주문 시 소비 |
| Member 적립/스탬프 | ✅ `develop` 기준 구현됨 (PR #7) | 주문 픽업완료 시 1개 적립, 10개 모으면 4500원 정액 할인으로 사용 |
| Notification | ➖ 백엔드 작업 불필요로 확인 (PR #8) | 프론트가 기존 SSE + 브라우저 Notification API만으로 이미 완결 구현 |
| Event | ⏸ 보류 (PR #9) | PRD 근거 없어 설계 불가 — 3절 참고 |
| Cart | ➖ 백엔드 작업 불필요로 확인 (PR #10) | 프론트가 Zustand `persist`(localStorage)로 클라이언트에만 보관, 서버 동기화 없음 |
| 메뉴 옵션 관리자 CRUD | ✅ `develop` 기준 구현됨 (PR #11) | 옵션 그룹/선택지 생성·수정·삭제·품절처리. 조회는 기존 `GET /menus/{id}`가 담당 |
| DB 마이그레이션(Flyway) | ✅ 도입 완료 (이 브랜치) | prod 전용 baseline(`V1__init.sql`), local/test는 기존 H2 ddl-auto 유지 — 2.3절 참고 |

## 2. 지금까지 한 일 (이 브랜치)

### 2.1 Package by Layer → Package by Feature 구조 전환
- **사용자 요청**: 도메인 우선순위 작업(로드맵 6번 진행 예정 시점)과 별개로, 패키지 구조 자체를
  `controllers/<domain>`, `dtos/<domain>`, `entities/<domain>`, `repositories/<domain>`,
  `services/<domain>`(레이어가 최상위) 방식에서 `<domain>/XxxController.kt`, `<domain>/XxxDto.kt`,
  `<domain>/Xxx.kt`, `<domain>/XxxRepository.kt`, `<domain>/XxxService.kt`(도메인이 최상위, 레이어는
  파일명 접미사) 방식으로 바꿔달라는 명시적 요청. 사용자 확인: 저장소 전체를 새 브랜치 하나로 한 번에
  이동, `CLAUDE.md`(루트)/`backend/CLAUDE.md` 문서도 함께 갱신.
- **PR #11(메뉴 옵션 관리자 CRUD)과의 순서 조율**: 이 리팩토링을 시작했을 때 PR #11이 구 구조 기준으로
  리뷰 대기 중이었음. 사용자와 상의해 "PR #11을 먼저 병합 → 그 다음 이 리팩토링 브랜치를 새 `develop`
  기준으로 재작업"하는 순서로 결정. 실제로 PR #11을 먼저 병합한 뒤, 이 브랜치를 삭제하고 새 `develop`에서
  다시 만들어 마이그레이션 스크립트를 재실행 — PR #11로 추가된 메뉴 옵션 CRUD 파일들도 자동으로 새
  구조에 포함됨(수동 conflict 해결 없이 깨끗하게 처리).
- **적용 범위**: `auth`, `category`, `coupon`, `member`, `menu`, `order`, `payment`, `store` 8개 도메인의
  `src/main/kotlin`·`src/test/kotlin` 전체 파일(60+ 파일)을 `git mv`로 이동. `auth/oauth/`(카카오/애플
  토큰 검증기)처럼 도메인 내부 하위 그룹은 서브패키지로 유지. `common/`, `config/`는 도메인이 아니라서
  그대로 둠.
- 파일 이동 후 모든 `package`/`import` 선언을 `com.gy.smartorder.(controllers|dtos|entities|repositories|
  services).<domain>` → `com.gy.smartorder.<domain>` 패턴으로 일괄 치환, 레이어가 같은 패키지로 합쳐지며
  생긴 불필요한 자기 자신 패키지 import를 전부 제거.
- `CLAUDE.md`(루트) "Backend architecture" 절과 `backend/CLAUDE.md`의 "[Directory & Architecture
  Conventions]" 절을 새 구조에 맞게 다시 씀.
- **검증**: `bash gradlew clean test` 전체 통과(`BUILD SUCCESSFUL`), 테스트 스위트 12개 총 95건 전부 통과
  — 리팩토링 전(develop, PR #11 병합 후 기준)과 동일 개수, 회귀 없음.

### 2.2 (참고) 메뉴 옵션 관리자 CRUD — PR #11로 `develop` 병합 완료
- 상세 구현 내역은 PR #11 및 이전 세션 기록 참고. 이 문서의 이전 절(옛 1.6절)에 있던 상세 내용은
  `develop` 병합 완료로 정리했다.
- **프론트 확인**: `StoreAdminDashboardRouter`엔 메뉴 품절 토글(`MenuSoldOutRow`)과 매장 오픈 스위치만 있고
  옵션 그룹/선택지 관리 UI는 없음. 다만 기존 `CategoryController`/`MenuController`가 프론트 admin UI가
  아직 안 쓰는 create/update/delete까지 이미 다 구현해둔 전례가 있어(Category/Menu 모두 풀 CRUD 보유),
  같은 패턴으로 옵션 CRUD도 미리 구현하기로 결정. `BACKEND_ROADMAP.md`도 "Category/Menu CRUD와 같은
  패턴으로 추가하면 됨"이라고 명시.
- 엔드포인트(신규, `controllers/menu/MenuOptionController.kt`):
  - `POST /menus/{menuId}/option-groups`, `PATCH /option-groups/{groupId}`, `DELETE /option-groups/{groupId}`
  - `POST /option-groups/{groupId}/choices`, `PATCH /option-choices/{choiceId}`, `DELETE /option-choices/{choiceId}`
  - `PATCH /option-choices/{choiceId}/sold-out` — 품절처리 전용(Menu의 `/status` 엔드포인트와 동일한 패턴)
  - 조회는 기존 `GET /menus/{menuId}` 응답에 이미 `optionGroups`가 포함돼 있어 별도 엔드포인트 없음.
- `services/menu/MenuOptionService.kt`(신규): Category/Menu 서비스와 동일한 관례 —
  `existsByMenuIdAndName`/`existsByOptionGroupIdAndLabel`로 그룹/그룹 내 선택지명 중복 생성 방지,
  `NotFoundException`(`MENU_NOT_FOUND`/`OPTION_GROUP_NOT_FOUND`/`OPTION_CHOICE_NOT_FOUND`)으로 404 처리.
- `type`(옵션 선택 방식) 요청 값은 프론트처럼 소문자 문자열("single"/"multiple")로 받고 서비스에서
  수동 파싱(`parseOptionType`) — 잘못된 값이면 `BadRequestException("INVALID_OPTION_TYPE")`. 기존
  `MenuOptionType` enum에 `@JsonCreator`를 붙이지 않은 이유(응답 직렬화와의 매핑 충돌 회피, 엔티티 주석
  참고)를 그대로 존중해 입력 파싱도 서비스 레이어에서 수동으로 처리.
- `entities/menu/MenuOptionGroup.kt`/`MenuOptionChoice.kt`에 `updateInfo()`/`updateSoldOut()` mutator
  추가(Menu/Category 엔티티와 동일한 관례).
- `repositories/menu/MenuOptionGroupRepository.kt`, `MenuOptionChoiceRepository.kt` 신규.
- `dtos/menu/MenuDto.kt`에 Create/Update/SoldOutUpdate 요청 DTO 6종 추가.
- **알려진 한계(기존 Category/Menu와 동일)**: `SecurityConfig`가 `/menus/**`, `/option-groups/**`,
  `/option-choices/**`를 STORE_ADMIN 역할로 제한하지 않고 있어(현재 `/stores/**`의 PATCH만 특별
  취급, 나머지는 `anyRequest, authenticated`로 인증만 요구) 로그인한 아무 회원이나 호출 가능함 — 이건
  이번에 새로 생긴 문제가 아니라 Category/Menu 변경 API가 원래도 갖고 있던 한계를 그대로 물려받은 것.
  STORE_ADMIN 제한이 필요하면 별도 작업으로 SecurityConfig 규칙 확장 필요.
- **검증**: `bash gradlew clean test` 전체 통과(`BUILD SUCCESSFUL`). 신규 `MenuOptionServiceTest`(12건:
  그룹 생성/중복명/메뉴없음/잘못된타입, 그룹 수정/없음, 그룹 삭제, 선택지 생성/중복명, 선택지 수정,
  선택지 품절처리, 선택지 삭제) 전부 통과, 기존 테스트 회귀 없음.

### 2.3 DB 마이그레이션(Flyway) 도입 — 완료
- **배경**: `backend/CLAUDE.md`/`BACKEND_ROADMAP.md`에 기록된 known gap — prod 프로필(`ddl-auto: validate`)이
  스키마를 직접 만들지 않는데도 그 스키마를 만들어줄 마이그레이션 도구가 없었음. `coupons`,
  `member.stamp_count`, 메뉴 옵션 그룹·선택지 테이블까지 추가돼 관리 대상이 더 늘어난 상태였음.
- **설계 결정**: local/test는 지금처럼 H2 + `ddl-auto`(각각 `update`/`create-drop`)로 엔티티 기반 자동
  스키마 생성을 유지하고, **prod에만 Flyway를 적용**하기로 함(로컬 개발 속도를 유지하면서 prod 스키마
  변경 이력만 버전 관리하는 절충 — 두 환경 모두 Flyway로 통일하는 것도 고려했으나, 개발 중 엔티티를
  자주 바꾸는 현재 단계에서 매번 마이그레이션 스크립트를 쓰는 비용이 더 크다고 판단).
- `build.gradle.kts`: `flyway-core`, `flyway-database-postgresql` 추가(버전은 Spring Boot BOM이 관리).
- `src/main/resources/application.yaml`: local 프로필에 `spring.flyway.enabled: false` 명시(플러그인이
  클래스패스에 올라오면 기본값이 `true`라 명시적으로 꺼야 함), prod 프로필에
  `spring.flyway.enabled: true` + `locations: classpath:db/migration` 명시.
- `src/test/resources/application-test.yaml`: 동일한 이유로 `spring.flyway.enabled: false` 추가.
- `src/main/resources/db/migration/V1__init.sql` 신규: 현재 11개 엔티티(Store/StoreAccount/Category/Menu/
  MenuOptionGroup/MenuOptionChoice/Member/Coupon/Order/OrderItem/Payment) 전체를 PostgreSQL DDL로 그대로
  스냅샷(이 브랜치의 `develop` 기준, 즉 PR #5~#12가 모두 반영된 flat 패키지 구조의 엔티티와 대조해 재검증
  완료). `Category.order` 컬럼은 Postgres 예약어라 `"order"`로 quoting. 인덱스/유니크 제약은 엔티티의
  `@Index`/`unique = true`를 그대로 반영했지만, `ddl-auto: validate`는 테이블/컬럼/타입/nullable만
  검증하고 인덱스 정의는 검증하지 않으므로 인덱스 이름·구성은 엔티티와 완전 동일할 필요는 없음(참고용으로만
  맞춤).
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test` 전체 통과(`BUILD SUCCESSFUL`, 95개
  테스트 전부 성공, 회귀 없음) — local/test 모두 Flyway가 꺼져 있어 기존 H2 기반 흐름에 영향 없음을 확인.
  실제 Postgres에 `V1__init.sql`을 적용해보는 검증은 로컬에 Postgres 인스턴스가 없어 못 함(다음
  prod/스테이징 배포 시 1회성으로 확인 필요).
- **후속 조치**: 이제부터 엔티티 변경 시 반드시 `Vn__*.sql` 마이그레이션을 함께 추가해야 함(더 이상
  `ddl-auto`가 prod 스키마를 대신 만들어주지 않음) — `backend/CLAUDE.md`/`CLAUDE.md`에 아직 이 규칙이
  명시돼 있지 않으니 별도 문서 동기화 작업에서 반영 예정.

### 2.4 (부수 발견) `develop`의 죽은 중복 테스트 파일 3개 제거
- `./gradlew test` 실행 중 `:compileTestKotlin`이 실패함을 발견. 원인: `src/test/kotlin/com/gy/smartorder/
  services/{member,menu,payment}/*ServiceTest.kt` 3개 파일이 옛 레이어 우선 구조(`entities.*`/`dtos.*`/
  `services.*` 패키지)를 참조하고 있었는데, 실제 메인 소스는 이미 flat 구조로 전환돼 해당 패키지가
  존재하지 않아 컴파일 자체가 깨져 있었음(즉 이 시점 `origin/develop`은 `./gradlew test`가 실패하는
  상태였음 — 2.1절 패키지 구조 전환과 PR #13 병합 과정에서 정리가 덜 된 잔재로 추정).
- `member`/`menu` 쪽은 이미 flat 경로(`member/MemberServiceTest.kt`, `menu/MenuOptionServiceTest.kt`)에
  최신 동작을 검증하는 정상 버전이 따로 존재해 완전히 중복이었음(특히 stray `services/member/
  MemberServiceTest.kt`는 스탬프 적립 트리거를 "결제 승인 시"로 가정하는 옛 설계를 테스트하고 있었는데,
  실제 현재 구현은 `OrderService.updateOrderStatus()`에서 주문이 `PICKED_UP`으로 바뀔 때 적립하는 방식으로
  이미 대체되어 있었음 — `OrderServiceTest.kt`가 이 동작을 이미 커버 중).
- `payment` 쪽(`services/payment/PaymentServiceTest.kt`)은 flat 경로에 대응 파일이 아예 없었지만, 이 파일이
  검증하던 "PaymentService가 결제 승인 시 `memberService.earnStamp()`를 호출한다"는 동작 자체가 현재
  `PaymentService`(생성자에 `memberService` 의존성 없음)에 더 이상 존재하지 않아 테스트 대상 자체가
  사라진 상태였음. 커버리지 손실 없이 삭제 가능하다고 판단.
- 세 파일 모두 `git rm`으로 삭제. 삭제 후 `./gradlew test` 재실행해 95개 테스트 전부 통과 확인.

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료 (PR #6, `develop` 병합됨)
### [x] 1. Member 적립/스탬프 도메인 — 완료 (PR #7, `develop` 병합됨)
### [x] 2. Notification 도메인 — 백엔드 작업 불필요로 확인 (PR #8, `develop` 병합됨)
### [보류] 3. Event 도메인 — PRD 근거 부족 (PR #9, `develop` 병합됨)
- PRD 맥락이 확보되기 전까지는 착수하지 않음.

### [x] 4. Cart 도메인 — 백엔드 작업 불필요로 확인 (PR #10, `develop` 병합됨)
### [x] 5. 메뉴 옵션 관리자 CRUD — 완료 (PR #11, `develop` 병합됨)
### [x] (번외) Package by Layer → Package by Feature 전환 — 완료, 2026-09-07 (`develop` 병합됨)
- 2.1절 참고. 로드맵 우선순위 목록에는 없던 사용자 직접 요청 작업.

### [x] 6-1. 마이그레이션 도구(Flyway) 도입 — 완료, 2026-09-21 (`feature/gy/db-migration` 브랜치)
- 2.3절 참고.

### [ ] 6-2. CI에서 `./gradlew build` 자동 검증
- 아직 착수 전. GitHub Actions 워크플로 신설 필요(별도 브랜치에서 진행 예정).

## 4. 리스크 / 확인 필요 항목
- **쿠폰 발급 경로가 웰컴 쿠폰뿐**: 관리자가 프로모션 쿠폰을 임의로 발급하는 기능은 아직 없음. 필요해지면
  별도 우선순위로(관리자 발급 API 신설 또는 `POST /stores/{storeId}/coupons` 등).
- **(해결됨, PR #6)** ~~Order 동시 요청 경쟁 상태로 쿠폰 이중 소비 방지가 불완전~~ — `ConflictException` 캐치 후
  재조회하는 방식으로 수정 완료.
- **스탬프 적립 시점은 PRD에 명시되지 않아 판단으로 결정함**: "주문이 `PICKED_UP`(픽업 완료) 상태로 전환될
  때 1개 적립"으로 구현(생성 시점이 아니라 완료 시점 — 취소된 주문에는 적립 안 됨). PRD/기획 의도와 다르면
  `OrderService.updateOrderStatus()`의 `justPickedUp` 조건만 바꾸면 됨.
- **DB 마이그레이션 도구 도입은 됐지만 실제 Postgres 적용 검증 안 됨**: `V1__init.sql`(2.3절)을 로컬에
  Postgres 인스턴스가 없어 실제로 띄워서 확인하지 못함. 다음 prod/스테이징 배포 시 최초 1회 Flyway가
  정상 적용되는지(그리고 Hibernate `ddl-auto: validate`가 통과하는지) 반드시 확인 필요.
- **엔티티 변경 시 마이그레이션 스크립트 동반 필수로 규칙이 바뀜**: Flyway 도입 이후 prod는 더 이상
  `ddl-auto`가 스키마를 대신 만들어주지 않으므로, 향후 엔티티 필드 추가/변경 작업은 `Vn__*.sql` 작성을
  빠뜨리지 않도록 주의(2.3절 후속 조치 참고).
