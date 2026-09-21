# 스마트오더 백엔드 진행 상황 & 다음 할 일

> 최종 갱신: 2026-09-21
> 이 문서는 프론트엔드 계약(타입/모의 API, `frontend/src/types`, `frontend/src/api`)과 실제 백엔드 코드
> (`backend/src/main/kotlin/com/gy/smartorder`)를 대조해 정리한 진행 상황 기록입니다. 진행하면서 체크박스를
> 갱신해주세요. 개별 PR 단위의 상세 구현 이력·의사결정 근거는 `backend/PROGRESS.md`를 참고하세요.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Store | ✅ 구현됨 | CRUD, Haversine 거리 계산, 커서 페이징 |
| Category | ✅ 구현됨 | CRUD |
| Menu | ✅ 구현됨 (옵션 그룹 포함) | 옵션 그룹/선택지 조회 + 주문 반영 + 관리자 CRUD(PR #11)까지 완료 |
| Order | ✅ 구현됨 | 생성/조회/상태변경/실시간추적(SSE)/쿠폰·스탬프 할인 반영 모두 완료 |
| Payment | 🔶 최소 구현 | 결제 승인 기록/멱등 처리만, PG 서버 측 실연동(재검증) 전 |
| Auth | ✅ 구현됨 | JWT 인프라, 소셜(카카오/애플)·SMS·매장관리자 로그인, `STORE_ADMIN`/`ADMIN` 인가, 리프레시 토큰(+로그아웃 무효화). 카카오/애플 실계정 발급·SMS 실발송 벤더 연동만 외부 의존으로 남음 |
| Coupon | ✅ 구현됨 (PR #6) | 회원가입 시 웰컴 쿠폰 자동 발급 + 주문 시 소비. 관리자가 임의로 프로모션 쿠폰을 발급하는 경로는 없음 |
| Member(적립/스탬프) | ✅ 구현됨 (PR #7) | 주문이 `PICKED_UP`으로 전환될 때 1개 적립, 10개 모으면 4,500원 정액 할인 |
| DB 마이그레이션(Flyway) | ✅ 구현됨 (PR #14) | prod 전용 baseline(`V1__init.sql`), local/test는 기존 H2 `ddl-auto` 유지 |
| Cart | ➖ 백엔드 불필요 확정 (PR #10) | 프론트가 Zustand `persist`(localStorage)로 클라이언트에만 보관, 체크아웃 시 바로 Order API로 변환 |
| Notification | ➖ 백엔드 불필요 확정 (PR #8) | 프론트가 기존 Order SSE + 브라우저 `Notification` API만으로 이미 완결 구현 |
| Event | ⏸ 보류 (PR #9) | 프론트 라우트/타입/API/mock 어디에도 대응 화면·계약이 없어 PRD 근거 확보 전까지 착수 보류 |
| CI | ❌ 미구현 | `.github/workflows`에 `./gradlew build` 자동 검증 파이프라인 없음 |

## 2. 다음에 할 일 (우선순위 순)

### [ ] 1. CI 파이프라인 구축
- GitHub Actions로 `push`/`pull_request`(base `develop`) 시 `backend/`에서 `./gradlew build` 자동 실행.
- (선택) `frontend/`용 `npm run lint` job 병행 추가.

### [ ] 2. Payment 도메인 — PG 서버 측 실연동
- 현재 `/payments/confirm`은 클라이언트가 보낸 금액/키를 주문 금액과만 대조할 뿐, PG사 서버에 실제로 그
  결제가 있었는지 재확인하지 않음 — 결제 위변조 리스크(4절 참고).

### [ ] 3. 메뉴 옵션 관리자 CRUD — SecurityConfig 권한 보강
- `/menus/**`, `/option-groups/**`, `/option-choices/**` 변경 엔드포인트가 아직 `STORE_ADMIN` 역할로
  제한돼 있지 않음(로그인한 아무 회원이나 호출 가능) — Category/Menu 변경 API가 원래 갖고 있던 한계를
  그대로 물려받은 것. `backend/PROGRESS.md` 2.2절 참고.

### [ ] 4. Auth 외부 계정 연동 마무리
- 카카오 앱/애플 Services ID 실계정 발급 후 엔드투엔드 검증, SMS 실발송 벤더 연동 — 둘 다 외부
  계정/과금 의존이라 코드 경로만 준비돼 있고 로컬에서 실토큰 검증은 못함.

### [보류] 5. Event 도메인
- PRD 맥락이 확보되기 전까지는 착수하지 않음.

### [ ] 6. Coupon 관리자 발급 API (필요해지면)
- 지금은 회원가입 웰컴 쿠폰 자동 발급뿐. 관리자가 임의로 프로모션 쿠폰을 발급하는 기능이 필요해지면
  `POST /stores/{storeId}/coupons` 등으로 신설.

## 3. 리스크 / 확인 필요 항목
- **PG 실연동 전 결제 위변조 리스크**: `/payments/confirm`이 PG사 서버에 실제 결제 여부를 재확인하지 않음.
- **DB 마이그레이션 도구 도입은 됐지만 실제 Postgres 적용 검증 안 됨**: `V1__init.sql`(PR #14)을 로컬에
  Postgres 인스턴스가 없어 실제로 띄워서 확인하지 못함. 다음 prod/스테이징 배포 시 최초 1회 Flyway가
  정상 적용되는지(그리고 `ddl-auto: validate`가 통과하는지) 반드시 확인 필요. 이후 엔티티 변경 시
  `Vn__*.sql` 마이그레이션을 함께 추가해야 함(더 이상 `ddl-auto`가 prod 스키마를 대신 만들어주지 않음).
- **SSE 구독자 레지스트리가 인스턴스 메모리에 있음**: 다중 인스턴스로 스케일아웃하면 Redis Pub/Sub으로
  브로드캐스트를 릴레이하는 구조로 바꿔야 함. 지금 단일 인스턴스 개발 단계라 문제 없음.
- **ID 타입 불일치 가능성**: 프론트는 모든 id를 `ID = string`으로 정의하지만, 백엔드는 요청 바디의
  `storeId`/`menuId`/`orderId` 등을 여전히 `Long`으로 받음. Jackson이 JSON 문자열을 `Long`으로 자동
  변환하지 않으면 실연동 시 400 에러 가능 — 프론트가 숫자 리터럴로 보내는지 확인 필요.
- **여러 작업 세션 동시 사용 이력**: 과거 로컬 Claude Code CLI 세션과 별도 클라우드(Cowork) 세션이 동시에
  이 저장소를 건드리며 동일 작업(메뉴 옵션 관리자 CRUD, Member 적립/스탬프, Cart/Event/Notification 확인)을
  중복 구현해 각각 별도 PR로 병합된 이력이 있음(모두 `develop`에 반영 완료, 코드 기능 자체는 정상). 앞으로
  여러 세션을 동시에 쓸 경우 브랜치/작업 범위를 먼저 조율할 것.
