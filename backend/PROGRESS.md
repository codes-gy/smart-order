# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-07 (1.7절 — 패키지 구조를 Package by Layer → Package by Feature로 전환)
> 기준 브랜치: `feature/gy/package-by-feature` (base: `develop`, merge-base에 PR #5~#11이 이미 병합돼 있음)
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

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료 (PR #6, `develop` 병합됨)
### [x] 1. Member 적립/스탬프 도메인 — 완료 (PR #7, `develop` 병합됨)
### [x] 2. Notification 도메인 — 백엔드 작업 불필요로 확인 (PR #8, `develop` 병합됨)
### [보류] 3. Event 도메인 — PRD 근거 부족 (PR #9, `develop` 병합됨)
- PRD 맥락이 확보되기 전까지는 착수하지 않음.

### [x] 4. Cart 도메인 — 백엔드 작업 불필요로 확인 (PR #10, `develop` 병합됨)
### [x] 5. 메뉴 옵션 관리자 CRUD — 완료 (PR #11, `develop` 병합됨)
### [x] (번외) Package by Layer → Package by Feature 전환 — 완료, 2026-09-07 (이 브랜치)
- 2.1절 참고. 로드맵 우선순위 목록에는 없던 사용자 직접 요청 작업.

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
