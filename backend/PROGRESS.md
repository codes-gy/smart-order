# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-07 (1.4절 — Event 도메인 PRD 근거 부족으로 보류)
> 기준 브랜치: `feature/gy/event` (base: `develop`, merge-base에 PR #5 Auth, PR #6 Coupon, PR #7 Member
> 적립/스탬프, PR #8 Notification 결론이 이미 병합돼 있음)
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
| Event | ⏸ 보류 (이 브랜치) | PRD 근거 없어 설계 불가 — 3절 참고 |

## 2. 지금까지 한 일 (이 브랜치)

### 2.1 Event 도메인 조사 — PRD 근거 부족으로 보류, 구현하지 않음
- **작업 안 함**: 코드 변경 없음.
- **조사 내용**: `BACKEND_ROADMAP.md`에 "Event | ❌ 미구현 | PRD상 용도 불명확, 우선순위 낮음" 한 줄 외
  아무 설명이 없음. 프론트엔드에 이벤트/프로모션 관련 페이지·타입·mock이 전혀 없음(`이벤트`, `promotion`,
  `banner` 등 전체 검색 결과 없음). 저장소 어디에도 PRD 원문이 없어(`find . -iname "*PRD*"` 결과 없음)
  Notification 때처럼 프론트 코드로 용도를 확정할 근거 자체가 없음.
- **사용자 확인**: 근거 없이 설계를 추측해서 구현하지 않고, 보류한 채 우선순위 4번(Cart 재확인)으로
  넘어가기로 결정. Event는 PRD 맥락이 확보되면 그때 다시 착수.

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료 (PR #6, `develop` 병합됨)
### [x] 1. Member 적립/스탬프 도메인 — 완료 (PR #7, `develop` 병합됨)
### [x] 2. Notification 도메인 — 백엔드 작업 불필요로 확인 (PR #8, `develop` 병합됨)
### [보류] 3. Event 도메인 — PRD 근거 부족, 2026-09-07 (이 브랜치)
- 2.1절 참고. PRD 맥락이 확보되기 전까지는 착수하지 않음.

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
