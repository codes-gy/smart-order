# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-07 (1.2절 — Member 적립/스탬프 도메인 구현 완료)
> 기준 브랜치: `feature/gy/member-stamp` (base: `develop`, merge-base에 PR #5 Auth, PR #6 Coupon이 이미 병합돼 있음)
> 이 문서는 로컬 Claude Code CLI 세션이 관리합니다. `BACKEND_ROADMAP.md`는 Cowork 세션이 별도로 관리하는
> 문서이니 혼동하지 말 것. 이전 브랜치(`feature/gy/auth`, `feature/gy/coupon`)의 작업 기록은 각각 PR #5, PR
> #6으로 병합 완료돼 이 문서에서는 정리했다 — 상세 이력은 `git log`/PR 참고.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Auth / Store / Category / Menu / Order / Payment | ✅ `develop` 기준 구현됨 | 이번 브랜치 시작 시점에 이미 병합돼 있음 |
| Coupon | ✅ `develop` 기준 구현됨 (PR #6) | 회원가입 시 웰컴 쿠폰 자동 발급 + 주문 시 소비 |
| Member 적립/스탬프 | ✅ 구현 완료 (이 브랜치) | 주문 픽업완료 시 1개 적립, 10개 모으면 4500원 정액 할인으로 사용 |

## 2. 지금까지 한 일 (이 브랜치)

### 2.1 Member 적립/스탬프 도메인 신설 — 구현 완료
- **프론트 계약 확인**: `frontend/src/utils/constants.ts`의 `STAMP_REWARD_DISCOUNT = 4500`(정액 할인),
  `frontend/src/types/auth.types.ts`의 `RewardsSummary.stampGoal`은 UI(`CouponStampSelector.tsx`)에서 10
  고정으로 취급, `orderMock.ts`는 `useStamp`면 4500원을 쿠폰 할인과 함께 차감. Coupon처럼 발급 이력이 필요한
  구조가 아니라 회원당 카운터 1개면 충분해서 별도 도메인 폴더 대신 **Member 엔티티 확장**으로 처리(별도
  `stamp` 도메인 폴더를 만들지 않기로 한 설계 결정).
- `entities/member/Member.kt`: `stampCount: Int`(기본 0) 필드 + `earnStamp()`(+1), `redeemStamp()`(0으로
  리셋) 메서드 추가.
- `services/member/MemberService.kt`:
  - `getRewards()`: `stampCount=0, stampGoal=10` placeholder를 `member.stampCount`/상수 `STAMP_GOAL=10`
    실값으로 교체.
  - `earnStamp(memberId)`(신규, `@Transactional`): 주문이 픽업 완료될 때 1개 적립.
  - `redeemStamp(memberId): Int`(신규, `@Transactional`): `stampCount >= STAMP_GOAL`(10) 검증 후 리셋하고
    고정 할인액 `STAMP_REWARD_DISCOUNT=4500` 반환. 목표치 미달이면 `ConflictException("STAMP_NOT_ENOUGH")` —
    Coupon의 `COUPON_ALREADY_USED`와 동일하게 "동시 요청이 방금 상태를 바꿨을 수도 있는" 종류의 실패라
    BadRequest가 아니라 Conflict로 던져 호출부가 레이스 처리를 할 수 있게 함.
- **적립 시점**: `OrderService.updateOrderStatus()`에서 상태가 (이전 상태와 다르게) `PICKED_UP`으로 바뀔
  때만 `memberService.earnStamp(order.memberId)` 호출. "이미 PICKED_UP인 주문을 다시 PICKED_UP으로 갱신"하는
  재요청이 들어와도 중복 적립되지 않도록 이전 상태를 먼저 캡처해 비교.
- **사용 시점**: `OrderService.createOrder()`에서 쿠폰 소비와 스탬프 소비를 하나의 `try/catch(ConflictException)`
  블록으로 묶어, `req.useStamp`면 `memberService.redeemStamp(memberId)`를 호출하고 쿠폰 할인액과 합산해
  `itemsAmount - couponDiscount - stampDiscount`(0원 아래로 내려가지 않음)를 `totalPrice`로 저장. 레이스 시
  기존 `couponService.redeem()`과 동일한 패턴으로 `orderRepository.findByIdempotencyKey()` 재조회 후 이미
  커밋된 주문을 멱등하게 반환.
- **검증**: `bash gradlew clean test` 전체 통과(`BUILD SUCCESSFUL`, 로컬에 JDK 25 툴체인이 없어 sdkman으로
  `25.0.4-tem` 설치 후 실행). 신규 `MemberServiceTest`(4건: 적립/사용성공/사용실패/조회), `OrderServiceTest`에
  스탬프 케이스 4건 추가(사용 성공, 동시요청 레이스 시 멱등 반환, PICKED_UP 전환 시 적립, 중복 전환 시
  미적립, 다른 상태 전환 시 미적립) — 총 `OrderServiceTest` 11건 + `MemberServiceTest` 4건 전부 통과, 기존
  테스트 회귀 없음.

### 2.2 커밋 + push + PR 생성 — 완료
- 커밋 2개로 분리: `feat: Member 적립/스탬프 도메인 추가 및 주문 연동`(코드+테스트), `docs: Member
  적립/스탬프 브랜치 진행 상황 정리`(이 문서). `git push -u origin feature/gy/member-stamp` 성공.
- 이번엔 `gh` CLI가 설치·인증돼 있어(`codes-gy` 계정) `gh pr create`로 바로 생성.
- **결과**: PR #7, `feature/gy/member-stamp` → `develop`, https://github.com/codes-gy/smart-order/pull/7
- **다음 단계**: 리뷰/머지 대기. 머지 후엔 3절 "다음에 할 일" 2번(Notification 도메인)부터 새 브랜치로
  이어서 진행하면 됨.

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료 (PR #6, `develop` 병합됨)
### [x] 1. Member 적립/스탬프 도메인 — 완료, 2026-09-07 (이 브랜치)
- 2.1절 참고.

### [ ] 2. Notification 도메인
- 결제/주문 상태 변경 시 서버발 푸시 트리거 필요 여부부터 재확인.

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
